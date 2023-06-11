package kvstore

import cats.effect._
import cats.effect.implicits._
import cats.effect.std.Queue
import cats.implicits._
import com.comcast.ip4s._
import kvstore.dtos.WSProtocol
import org.http4s.ember.server._
import org.http4s.server._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.http4s.server.middleware

import scala.concurrent.duration._
import Http._
import org.http4s.dsl.Http4sDsl
import fs2.io.net.Network

object Service {

  def serviceR[F[_]: Network: std.Console](implicit
      F: Async[F]
  ): Resource[F, Server] = {

    val dsl = Http4sDsl[F]
    import dsl._

    for {

      logger <- Slf4jLogger.create[F].toResource

      kvStore <- KvStore.make[F, String, String](1000)

      httpRoutesWithLoggerMiddleware = middleware.Logger.httpRoutes[F](
        logHeaders = false,
        logBody = true,
        redactHeadersWhen = _ => false,
        logAction = ((msg: String) => std.Console[F].println(msg)).some
      )(
        Router(
          "api" -> (new AliveRoutes[F]().routes <+> new KvStoreRoutes[F](
            kvStore
          ).routes)
        )
      )

      host <- Resource.eval {
        F.fromOption(
          Host.fromString("localhost"),
          new IllegalArgumentException("Invalid host")
        )
      }

      port <- Resource.eval {
        F.fromOption(
          Port.fromInt(8090),
          new IllegalArgumentException("Invalid port")
        )
      }

      outMessages <- Queue.unbounded[F, WSProtocol.Server].toResource

      // watch changes in store size
      _ <- kvStore.size.discrete.changes
        .evalMap { _ =>
          kvStore.entries.flatMap { entries =>
            outMessages.offer(WSProtocol.Server.KeyValueEntries(entries))
          }
        }
        .compile
        .drain
        .background

      receivePipe = (inStream: fs2.Stream[F, WSProtocol.Client]) =>
        inStream.evalMap { case WSProtocol.Client.Ping =>
          outMessages.offer(WSProtocol.Server.Pong)
        }

      server <- EmberServerBuilder
        .default[F]
        .withHost(host)
        .withPort(port)
        .withHttpWebSocketApp(ws =>
          (new Websocket(
            ws,
            outMessages,
            receivePipe
          ).routes <+> httpRoutesWithLoggerMiddleware).orNotFound
        )
        .withMaxConnections(32)
        .withIdleTimeout(10.seconds)
        .withLogger(logger) // use ?
        .withErrorHandler(error => BadRequest(error.getMessage)) // improve ?
        .build
        .evalTap { _ => logger.info(s"Server listennig on $host:$port") }

    } yield server
  }

}

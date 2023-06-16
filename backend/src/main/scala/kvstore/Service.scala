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

  def serviceR[F[_]: Network](implicit
      F: Async[F]
  ): Resource[F, Server] = {

    val dsl = Http4sDsl[F]
    import dsl._

    for {

      logger <- Slf4jLogger.create[F].toResource

      kvStore <- KvStore.make[F, String, String](1000)

      tableUpdate <- fs2.concurrent.SignallingRef
        .of[F, Boolean](true)
        .toResource

      httpRoutes = middleware.CORS.policy
        .withAllowOriginAll(
          middleware.Logger.httpRoutes[F](
            logHeaders = false,
            logBody = true,
            logAction = ((msg: String) => logger.info(msg)).some
          )(
            Router(
              "api" -> (new AliveRoutes[F]().routes <+> new KvStoreRoutes[F](
                kvStore, tableUpdate
              ).routes)
            )
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

      // queue for outgoing messages
      outMessages <- Queue.unbounded[F, WSProtocol.Server].toResource

      searchKeyR <- F.ref(none[String]).toResource

      offerFilteredEntries = searchKeyR.get.flatMap {
        _.fold(kvStore.entries)(sKey =>
          kvStore.entries.map(_.filter { case (key, _) =>
            key.contains(sKey)
          })
        ).flatMap { entries =>
          outMessages.offer(WSProtocol.Server.KeyValueEntries(entries))
        }
      }

      // watch changes in store size
      _ <- tableUpdate.discrete.changes
        .evalMap { _ => offerFilteredEntries *> F.delay { println("Changed!")} }
        .compile
        .drain
        .background

      receivePipe = (inStream: fs2.Stream[F, WSProtocol.Client]) =>
        inStream.evalMap {
          case WSProtocol.Client.Ping =>
            outMessages.offer(WSProtocol.Server.Pong)

          case WSProtocol.Client.SearchKey(searchKey) =>
            searchKeyR.set(searchKey) *> offerFilteredEntries
        }

      server <- EmberServerBuilder
        .default[F]
        .withHost(host)
        .withPort(port)
        .withHttpWebSocketApp(websocketBuilder =>
          (new Websocket(
            websocketBuilder,
            outMessages,
            receivePipe
          ).routes <+> httpRoutes).orNotFound
        )
        .withMaxConnections(32)
        .withIdleTimeout(10.seconds)
        .withErrorHandler(error =>
          BadRequest(error.getMessage)
        ) // TODO: improve ?
        .build
        .evalTap { _ => logger.info(s"Server listennig on $host:$port") }

    } yield server
  }

}

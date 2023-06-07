package kvstore

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import com.comcast.ip4s._
import fs2.io.net.Network
import org.http4s.ember.server._
import org.http4s.server._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._

import Http._
import cats.effect.std.Queue
import kvstore.dtos.WSProtocol

object Service extends IOApp {

  def serviceR[F[_]: Network](implicit F: Async[F]): Resource[F, Server] = for {

    logger <- Slf4jLogger.create[F].toResource

    kvStore <- KvStore.make[F, String, String](5)

    httpRoutes = Router(
      "api" -> (new AliveRoutes[F]().routes <+> new KvStoreRoutes[F](
        kvStore
      ).routes)
    )

    host <- Resource.eval {
      F.fromOption(
        Host.fromString("0.0.0.0"),
        new IllegalArgumentException("Invalid host")
      )
    }

    port <- Resource.eval {
      F.fromOption(
        Port.fromInt(8090),
        new IllegalArgumentException("Invalid port")
      )
    }

    _ <- logger.info(s"Server running on $host:$port").toResource

    outMessages <- Queue.unbounded[F, WSProtocol.Server].toResource

    server <- EmberServerBuilder
      .default[F]
      .withHost(host)
      .withPort(port)
      .withHttpWebSocketApp(ws =>
        (new Websocket(ws, outMessages).routes <+> httpRoutes).orNotFound
      )
      .withMaxConnections(32)
      .withIdleTimeout(10.seconds)
      .build

    _ <- fs2.Stream
      .fixedDelay(1.seconds)
      .evalMap { _ =>
        kvStore.entries.map { entries =>
          outMessages.offer(WSProtocol.Server.KeyValueEntries(entries))
        }
      }
      .compile
      .drain
      .background

  } yield server

  def useServer[F[_]: Network](implicit F: Async[F]): F[Unit] =
    serviceR.use(_ => F.never)

  override def run(args: List[String]): IO[ExitCode] =
    useServer[IO].as(ExitCode.Success)
}

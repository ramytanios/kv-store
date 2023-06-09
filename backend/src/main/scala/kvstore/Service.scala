package kvstore

import cats.effect._
import cats.effect.implicits._
import cats.effect.std.Queue
import cats.implicits._
import com.comcast.ip4s._
import fs2.io.net.Network
import kvstore.dtos.WSProtocol
import org.http4s.ember.server._
import org.http4s.server._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._

import Http._

object Service {

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

    _ <- logger
      .info(s"Server running on $host:$port")
      .toResource // does not work

    _ <- F.delay {
      println(s"Server running on $host:$port")
    }.toResource // works

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

  } yield server

}

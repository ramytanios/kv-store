package kvstore

import cats.effect._
import cats.syntax.semigroupk._
import org.http4s.ember.server._
import com.comcast.ip4s._
import org.http4s._
import Routes._
import org.http4s.server._

object Service extends IOApp {

  def serviceR[F[_]: Async]: Resource[F, Server] = for {

    kvStore <- KvStore.make[F, String, String](5)

    routes = new AliveRoutes[F]().routes <+> new KvStoreRoutes[F](
      kvStore
    ).routes

    httpApp = routes.orNotFound

    server <- EmberServerBuilder
      .default[F]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8090")
      .withHttpApp(httpApp)
      .build

  } yield server

  override def run(args: List[String]): IO[ExitCode] =
    serviceR[IO].use(_ => IO.never).as(ExitCode.Success)
}

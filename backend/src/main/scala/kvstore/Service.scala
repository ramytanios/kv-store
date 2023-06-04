package kvstore

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import org.http4s.server.Router
import cats.syntax.semigroupk._
import org.http4s.ember.server._
import com.comcast.ip4s._
import org.http4s.HttpRoutes
import cats.effect.Async
import cats.data.Kleisli
import org.http4s.{Request, Response}

object Service extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8090")
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)

  }

}

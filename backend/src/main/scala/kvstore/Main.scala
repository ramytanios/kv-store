package kvstore

import cats.effect.IOApp
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import org.http4s.server.Server
import cats.effect.IO
import cats.effect.ExitCode

object Main extends IOApp {

  def useServer[F[_]](serverR: Resource[F, Server])(implicit
      F: Async[F]
  ): F[Unit] =
    serverR.use(_ => F.never)

  override def run(args: List[String]): IO[ExitCode] =
    useServer[IO](Service.serviceR[IO]).as(ExitCode.Success)
}

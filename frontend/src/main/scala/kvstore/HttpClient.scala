package kvstore

import io.circe.Decoder
import io.circe.Encoder
import cats.syntax.all._
import cats.effect.Async
import org.http4s.dom.FetchClientBuilder
import org.http4s.Uri
import org.http4s.circe.CirceEntityCodec._
import org.http4s.Method
import org.http4s.client.dsl.Http4sClientDsl

trait HttpClient[F[_]] {

  /** get request */
  def get[R: Decoder](url: String): F[R]

  /** post request */
  def post[P: Encoder, R: Decoder](url: String, payload: P): F[R]

  /** delete request */
  def delete(url: String): F[Unit]
}

object HttpClient {
  def apply[F[_]](implicit F: Async[F]): HttpClient[F] = {
    val client = FetchClientBuilder[F].create

    val dsl = Http4sClientDsl[F]
    import dsl._

    new HttpClient[F] {
      override def get[R: Decoder](url: String): F[R] =
        F.fromEither(Uri.fromString(url)).flatMap { uri =>
          client.expect[R](Method.GET(uri))
        }

      override def post[P: Encoder, R: Decoder](url: String, payload: P): F[R] =
        F.fromEither(Uri.fromString(url)).flatMap { uri =>
          client.expect[R](Method.POST(payload, uri))
        }

      override def delete(url: String): F[Unit] =
        F.fromEither(Uri.fromString(url)).flatMap { uri =>
          client.expect[Unit](Method.DELETE(uri))
        }
    }

  }

}

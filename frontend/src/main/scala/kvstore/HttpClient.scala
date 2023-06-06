package kvstore

import cats.effect.kernel.Async
import io.circe.Decoder
import io.circe.Encoder

trait HttpClient[F[_]] extends ff4s.HttpClient[F] {

  def delete(url: String, id: String): F[Unit]

}

object HttpClient {}

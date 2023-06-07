package kvstore

trait HttpClient[F[_]] extends ff4s.HttpClient[F] {

  def delete(url: String, id: String): F[Unit]

}

object HttpClient {}

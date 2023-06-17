package kvstore

import cats._
import cats.effect._
import cats.implicits._
import kvstore.dtos.Dtos._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

object Http {

  /** backend alive http endpoints */
  final class AliveRoutes[F[_]: Monad]() extends Http4sDsl[F] {

    private val prefixPath = "/alive"

    private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
      case GET -> Root =>
        Ok("I am alive")
    }

    val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
  }

  /** key value store http endpoints */
  final class KvStoreRoutes[F[_]: Concurrent](
      store: KvStore[F, String, String]
  ) extends Http4sDsl[F] {

    private val prefixPath = "/kv"

    private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
      // return all entries of the store (sorted wrt key)
      case GET -> Root =>
        Ok(store.entries.map(_.map { case (key, value) =>
          KeyValue(key, value)
        }))

      // insert a key value pair in the store
      case req @ POST -> Root =>
        // val responseF =
        for {
          keyValue <- req.as[KeyValue]
          _ <- store.insert(keyValue.key, keyValue.value)
          response <- Ok()
        } yield response

      // delete a specific kv pair
      case DELETE -> Root / key =>
        store
          .remove(key)
          .flatMap(_ => Ok())

      // clear the store
      case DELETE -> Root =>
        store.clear.flatMap(_ => Ok())
    }

    val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
  }

}

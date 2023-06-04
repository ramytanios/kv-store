package kvstore

import org.http4s.dsl.Http4sDsl
import org.http4s._
import org.http4s.server.Router
import cats._
import org.http4s.circe._
import io.circe._
import io.circe.generic.semiauto._
import cats.effect._
import cats.effect.implicits._
import cats.implicits._

object Routes {

  /** Backend alive http endpoints */
  final class AliveRoutes[F[_]: Monad]() extends Http4sDsl[F] {
    private val prefixPath = "/alive"

    private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
      case GET -> Root =>
        Ok("I am alive")
    }

    val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
  }

  /** Key value store http endpoints */
  final class KvStoreRoutes[F[_]: Concurrent](store: KvStore[F, String, String])
      extends Http4sDsl[F] {
    private val prefixPath = "/store"

    // TODO: Move to dtos
    case class KeyValue(key: String, value: String)
    object KeyValue {
      implicit val codec: Codec[KeyValue] = deriveCodec
    }

    implicit val keyValueEntityEncoder: EntityEncoder[F, List[KeyValue]] =
      jsonEncoderOf[F, List[KeyValue]]

    implicit val keyValueEntityDecoder: EntityDecoder[F, KeyValue] =
      jsonOf[F, KeyValue]

    private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
      // get all entries of the store
      case GET -> Root =>
        Ok(store.entries.map(_.map { case (key, value) =>
          KeyValue(key, value)
        }))

      // insert a key value pair in the store
      case req @ POST -> Root =>
        for {
          keyValue <- req.as[KeyValue]
          _ <- store.insert(keyValue.key, keyValue.value)
          response <- Ok()
        } yield response
    }

    val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
  }

}

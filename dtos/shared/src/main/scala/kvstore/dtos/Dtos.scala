package kvstore.dtos

import io.circe._
import io.circe.generic.semiauto._

object Dtos {

  case class KeyValue(key: String, value: String)

  object KeyValue {
    implicit val codec: Codec[KeyValue] = deriveCodec
  }

}

package kvstore.dtos

import io.circe._
import io.circe.generic.semiauto._

sealed trait WSProtocol

object WSProtocol {

  sealed trait Client extends WSProtocol

  implicit val clientCodec: Codec[Client] = deriveCodec

  object Client {

    case object Ping extends Client
    case class SearchKey(key: Option[String]) extends Client

  }

  sealed trait Server extends WSProtocol

  implicit val serverCodec: Codec[Server] = deriveCodec

  object Server {

    case object Pong extends Server
    case class KeyValueEntries(kvs: List[(String, String)]) extends Server

  }

}

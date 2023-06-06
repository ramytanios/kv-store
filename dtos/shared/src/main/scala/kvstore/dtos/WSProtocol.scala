package kvstore.dtos

sealed trait WSProtocol

object WSProtocol {
  object Client {
    case object Ping extends WSProtocol
  }

  object Server {
    case object Pong extends WSProtocol
  }

}

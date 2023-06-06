package kvstore.dtos

sealed trait WSProtocol

object WSProtocol {

  sealed trait Client extends WSProtocol
  object Client {
    case object Ping extends Client
  }

  sealed trait Server extends WSProtocol
  object Server {
    case object Pong extends Server
  }

}

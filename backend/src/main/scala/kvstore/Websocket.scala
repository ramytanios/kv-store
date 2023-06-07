package kvstore

import cats.effect.Async
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import cats.effect.std.Queue
import kvstore.dtos.WSProtocol

class Websocket[F[_]](
    ws: WebSocketBuilder2[F],
    sendQueue: Queue[F, WSProtocol.Server]
)(implicit F: Async[F])
    extends Http4sDsl[F] {

  private val prefixPath = "/ws"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    val send: fs2.Stream[F, WebSocketFrame] =
      fs2.Stream
        .fromQueueUnterminated(sendQueue)
        .map(message => WebSocketFrame.Text(message.asJson.noSpaces))

    val receive: fs2.Pipe[F, WebSocketFrame, Unit] =
      _.evalMap(_ => F.unit)

    ws.build(send, receive)
  }

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)

}

package kvstore

import cats.effect._
import cats.effect.implicits._
import cats.effect.std.Queue
import cats.syntax.all._
import kvstore.dtos.Dtos._
import kvstore.dtos.WSProtocol

import scala.concurrent.duration._
import java.{util => ju}

object Store {

  def apply[F[_]](implicit
      F: Async[F],
      console: std.Console[F]
  ): Resource[F, ff4s.Store[F, State, Action]] = {

    val httpClient = HttpClient[F]

    val backendUrl = "localhost:8090/api/kv"

    for {
      outMessages <- Queue.unbounded[F, WSProtocol.Client].toResource

      store <- ff4s.Store[F, State, Action](State.default) { _ =>
        _ match {
          case Action.SetAlive(alive) => _.copy(alive = alive) -> none

          case Action.SetWsOpen(wsOpen) => _.copy(wsOpen = wsOpen) -> none

          case Action.SetKey(key) => _.copy(key = key) -> none

          case Action.SetValue(value) => _.copy(value = value) -> none

          case Action.SetKvEntries(entries) =>
            _.copy(kvEntries = entries) -> none

          case Action.InsertKeyValue =>
            state =>
              state -> (for {
                key <- F.fromOption(state.key, new ju.NoSuchElementException)
                value <- F.fromOption(
                  state.value,
                  new ju.NoSuchElementException
                )
                _ <- httpClient
                  .post[KeyValue, Unit](backendUrl, KeyValue(key, value))
              } yield ()).handleErrorWith(error => console.print(error.getMessage)).some

          case Action.ClearStore =>
            _ -> httpClient.delete(backendUrl).some

          case Action.RemoveKeyValue(key) =>
            _ -> httpClient.delete(s"$backendUrl/$key").some
        }
      }

      _ <- fs2.Stream
        .fixedDelay(5.seconds)
        .evalMap { _ => outMessages.offer(WSProtocol.Client.Ping) }
        .compile
        .drain
        .background

      _ <- ff4s
        .WebSocketClient[F]
        .bidirectionalJson[WSProtocol.Server, WSProtocol.Client](
          "ws://localhost:8090/ws",
          _.evalMap {
            case WSProtocol.Server.Pong =>
              console.println("Pong received") *> store.dispatch(
                Action.SetWsOpen(true)
              )
            case WSProtocol.Server.KeyValueEntries(entries) =>
              store.dispatch(Action.SetKvEntries(entries))
          },
          fs2.Stream.fromQueueUnterminated(outMessages)
        )
        .background

    } yield store
  }
}

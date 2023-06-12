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
    val backendBaseUrl = "127.0.0.1:8090"
    val httpUrl = s"http://$backendBaseUrl/api/kv"
    val wsUrl = s"ws://$backendBaseUrl/ws"

    for {
      outMessages <- Queue.unbounded[F, WSProtocol.Client].toResource

      store <- ff4s.Store[F, State, Action](State.default) { _ =>
        _ match {

          case Action.SetWsOpen(wsOpen) => _.copy(wsOpen = wsOpen) -> none

          case Action.SetKey(key) => _.copy(key = key) -> none

          case Action.SetValue(value) => _.copy(value = value) -> none

          case Action.SetSearchKey(key) => _.copy(searchKey = key) -> none

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
                  .post[KeyValue, Unit](httpUrl, KeyValue(key, value))
              } yield ())
                .handleErrorWith(error => console.print(error.getMessage))
                .some

          case Action.ClearStore =>
            _ -> httpClient.delete(httpUrl).some

          case Action.RemoveKeyValue(key) =>
            _ -> httpClient.delete(s"$httpUrl/$key").some
        }
      }

      // needed for one page is refreshed is pages,
      // the size is not detected to change and hence the table of entries is empty
      _ <- httpClient
        .get[List[KeyValue]](httpUrl)
        .flatMap { entries =>
          store.dispatch(
            Action.SetKvEntries(entries.map(pair => (pair.key, pair.value)))
          )
        }
        .toResource

      // ping the backend every 5 seconds
      _ <- fs2.Stream
        .fixedDelay(5.seconds)
        .evalMap { _ => outMessages.offer(WSProtocol.Client.Ping) }
        .compile
        .drain
        .background

      // watch search key changes
      _ <- store.state
        .map(_.searchKey)
        .changes
        .discrete
        .debounce(1.seconds) // throttle
        .evalMap { searchKey =>
          outMessages.offer(WSProtocol.Client.SearchKey(searchKey))
        }
        .compile
        .drain
        .background

      // websocket
      _ <- ff4s
        .WebSocketClient[F]
        .bidirectionalJson[WSProtocol.Server, WSProtocol.Client](
          wsUrl,
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

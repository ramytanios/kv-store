package kvstore

import cats.effect._
import cats.syntax.all._
import kvstore.dtos.Dtos._

object Store {

  def apply[F[_]](implicit
      F: Async[F]
  ): Resource[F, ff4s.Store[F, State, Action]] = {

    val httpClient = HttpClient[F]

    val backendUrl = "localhost:8090/api/kv"

    ff4s.Store[F, State, Action](State.default) { _ =>
      _ match {
        case Action.SetAlive(alive)       => _.copy(alive = alive) -> none

        case Action.SetWsOpen(wsOpen)     => _.copy(wsOpen = wsOpen) -> none

        case Action.SetKey(key)           => _.copy(key = key) -> none

        case Action.SetValue(value)       => _.copy(value = value) -> none

        case Action.SetKvEntries(entries) => _.copy(kvEntries = entries) -> none

        case Action.InsertKeyValue(key, value) =>
          _ -> httpClient
            .post[KeyValue, Unit](backendUrl, KeyValue(key, value))
            .some

        case Action.ClearStore =>
          _ -> httpClient.delete(backendUrl).some

        case Action.RemoveKeyValue(key) =>
          _ -> httpClient.delete(s"$backendUrl/$key").some
      }
    }

  }
}

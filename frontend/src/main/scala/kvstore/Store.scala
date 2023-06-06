package kvstore

import cats.effect._
import cats.syntax.all._

object Store {

  def apply[F[_]](implicit
      F: Async[F]
  ): Resource[F, ff4s.Store[F, State, Action]] =
    ff4s.Store[F, State, Action](State.default) { store =>
      _ match {
        case Action.SetAlive(alive)       => _.copy(alive = alive) -> none
        case Action.SetWsOpen(wsOpen)     => _.copy(wsOpen = wsOpen) -> none
        case Action.SetKey(key)           => _.copy(key = key) -> none
        case Action.SetValue(value)       => _.copy(value = value) -> none
        case Action.SetKvEntries(entries) => _.copy(kvEntries = entries) -> none
      }
    }

}

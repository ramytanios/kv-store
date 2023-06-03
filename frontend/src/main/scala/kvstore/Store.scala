package kvstore

import cats.effect._
import cats.syntax.all._

object Store {

  def apply[F[_]](implicit
      F: Async[F]
  ): Resource[F, ff4s.Store[F, State, Action]] =
    ff4s.Store[F, State, Action](State.default) { store =>
      _ match {
        case Action.DoSomething => state => state -> none
      }
    }

}

package kvstore

import cats.effect.kernel.Async
import cats.effect.std

class App[F[_]: std.Console](implicit F: Async[F]) extends ff4s.App[F, State, Action] {
  override val store = Store[F]
  override val view = View[F]
}

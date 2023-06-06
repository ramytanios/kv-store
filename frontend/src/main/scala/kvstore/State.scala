package kvstore

import cats.syntax.all._

final case class State(
    alive: Boolean = true,
    wsOpen: Boolean = true,
    key: Option[String] = none[String],
    value: Option[String] = none[String],
    kvEntries: List[(String, String)] = Nil
)

object State {
  val default: State = State()
}

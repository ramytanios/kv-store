package kvstore

final case class State(alive: Boolean = true)

object State {
  val default: State = State()
}

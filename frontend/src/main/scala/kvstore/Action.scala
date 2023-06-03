package kvstore

sealed trait Action

object Action {

  case object DoSomething extends Action
}

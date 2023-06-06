package kvstore

sealed trait Action

object Action {

  case class SetAlive(isAlive: Boolean) extends Action

  case class SetWsOpen(isOpen: Boolean) extends Action

  case class SetKey(key: Option[String]) extends Action

  case class SetValue(value: Option[String]) extends Action

  case class SetKvEntries(kvs: List[(String, String)]) extends Action

  // case class InsertKeyValue(key: String, value: String) extends Action
}

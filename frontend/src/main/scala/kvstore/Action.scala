package kvstore

sealed trait Action

object Action {

  case class SetWsOpen(isOpen: Boolean) extends Action

  case class SetKey(key: Option[String]) extends Action

  case class SetValue(value: Option[String]) extends Action

  case class SetSearchKey(key: Option[String]) extends Action

  case class SetKvEntries(kvs: List[(String, String)]) extends Action

  case object InsertKeyValue extends Action

  case class RemoveKeyValue(key: String) extends Action

  case object ClearStore extends Action
}

package kvstore

import cats.syntax.all._

object View {
  def apply[F[_]](implicit dsl: ff4s.Dsl[F, State, Action]) = {

    import dsl._
    import dsl.html._

    val components = new Components[F, State, Action]
    import components._

    useState { state =>
      div(
        cls := "bg-zinc-500 h-screen w-full",
        customBtn("Insert", _ => Action.InsertKeyValue.some, _ => false),
        customText(
          state.key.getOrElse(""),
          "key",
          (_, textStr) => Action.SetKey(textStr.some).some
        ),
        customText(
          state.value.getOrElse(""),
          "value",
          (_, textStr) => Action.SetValue(textStr.some).some
        ),
        div(state.kvEntries.mkString("||"))
      )
    }

  }

}

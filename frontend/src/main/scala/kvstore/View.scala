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
        cls := "flex flex-col h-screen overflow-hidden bg-zinc-200 h-screen font-mono uppercase",
        headerTag(
          cls := "w-full text-center border-b p-4 bg-zinc-500 text-xl",
          "key value store"
        ),
        mainTag(
          cls := "flex-1 overflow-y-scroll p-2",
          customBtn("Insert", _ => Action.InsertKeyValue.some, _ => false),
          div(
            cls := "flex",
            customText(
              state.key,
              "i.e: Foo",
              (_, textStr) => Action.SetKey(textStr.some).some
            ),
            customText(
              state.value,
              "i.e: {'name': 'bar' 'age': 20 }",
              (_, textStr) => Action.SetValue(textStr.some).some
            )
          ),
          customTable[String](
            List("Key", "Value"),
            state.kvEntries.map { case pair =>
              (pair._1, List(pair._1, pair._2))
            },
            _ => None,
            None
          ),
        ),
        footerTag(
          cls := "w-full text-center border-t p-4 bg-zinc-500 text-xl",
          "typelevel based"
        )
      )
    }

  }

}

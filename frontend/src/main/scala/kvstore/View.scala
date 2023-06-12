package kvstore

import cats.syntax.all._

object View {
  def apply[F[_]](implicit dsl: ff4s.Dsl[F, State, Action]) = {

    import dsl._
    import dsl.html._

    val components = new Components[F, State, Action]
    import components._

    val keySvg = literal("""<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 5.25a3 3 0 013 3m3 0a6 6 0 01-7.029 5.912c-.563-.097-1.159.026-1.563.43L10.5 17.25H8.25v2.25H6v2.25H2.25v-2.818c0-.597.237-1.17.659-1.591l6.499-6.499c.404-.404.527-1 .43-1.563A6 6 0 1121.75 8.25z" />
</svg>""")

    useState { state =>
      div(
        cls := "flex flex-col h-screen overflow-hidden bg-zinc-200 h-screen font-mono uppercase",
        headerTag(
          cls := "w-full border-b p-4 bg-zinc-500 text-xl flex justify-center align-center",
          span("key value store"),
          span(
            cls := "relative flex w-2 h-2",
            span(
              cls := s"w-full h-full absolute animate-ping rounded-full ${if (state.wsOpen) "bg-green-500"
                else "bg-red-500"}"
            ),
            span(cls := s"relative h-2 w-2 rounded-full ${if (state.wsOpen) "bg-green-500"
              else "bg-red-500"}")
          )
        ),
        mainTag(
          cls := "flex-1 overflow-y-scroll p-2 grid grid-cols-2",
          div(
            cls := "flex flex-col justify-center align-center mx-2",
            div(
              cls := "shrink mb-1 flex flex-col",
              span("Search key"),
              customInput(
                state.searchKey,
                "",
                (_, searchStr) => Action.SetSearchKey(searchStr.some).some
              )
            ),
            customTable[String](
              List("Key", "Value"),
              state.kvEntries.map { case pair =>
                (pair._1, List(pair._1, pair._2))
              },
              _ => None,
              None
            )
          ),
          div(
            div(
              cls := "flex flex-1 justify-between align-center",
              customBtn("Insert", _ => Action.InsertKeyValue.some, _ => false),
              div(
                cls := "border rounded border-black w-fit flex items-center justify-center",
                s"${state.kvEntries.size.toString}",
                span(cls := "left-0", keySvg)
              )
            ),
            div(
              cls := "flex mt-1 align-center",
              div(
                cls := "mr-1 grow flex flex-col",
                span("Key"),
                customText(
                  state.key,
                  "i.e: some key",
                  (_, textStr) => Action.SetKey(textStr.some).some
                )
              ),
              div(
                cls := "grow flex flex-col",
                span("Value"),
                customText(
                  state.value,
                  "i.e: {'name': 'bar' 'age': 20 }",
                  (_, textStr) => Action.SetValue(textStr.some).some
                )
              )
            )
          )
        ),
        footerTag(
          cls := "w-full text-center border-t p-4 bg-zinc-500 text-xl",
          "typelevel based"
        )
      )
    }
  }
}

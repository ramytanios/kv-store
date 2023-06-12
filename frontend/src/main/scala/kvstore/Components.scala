package kvstore

import org.scalajs.dom
import cats.kernel.Eq
import cats.syntax.all._

class Components[F[_], S, A] {

  // custom button
  def customBtn(
      label0: String,
      onClick0: S => Option[A],
      isDisabled: S => Boolean
  )(implicit dsl: ff4s.Dsl[F, S, A]): dsl.V = {
    import dsl._
    import dsl.html._

    useState { state =>
      button(
        cls := "border border-black rounded px-2 py-1 hover:bg-zinc-300 transition active:scale-90 disabled:opacity-50 disabled:pointer-events-none",
        styleAttr := "text-transform: inherit",
        label0,
        disabled := isDisabled(state),
        onClick := (_ => onClick0(state))
      )
    }
  }

  // custom text
  def customText(
      input0: Option[String],
      placeholder0: String,
      onInput0: (S, String) => Option[A]
  )(implicit
      dsl: ff4s.Dsl[F, S, A]
  ): dsl.V = {
    import dsl._
    import dsl.html._

    useState { state =>
      textArea(
        cls := "rounded-md border outline-none w-full h-full overall-y-scroll",
        placeholder := placeholder0,
        styleAttr := "resize: none",
        onInput := ((ev: dom.Event) =>
          ev.target match {
            case el: dom.HTMLTextAreaElement => onInput0(state, el.value)
            case _                           => None
          }
        ),
        value := input0.getOrElse("")
      )
    }
  }

  // custom table
  def customTable[RowKey: Eq](
      colNames: List[String],
      tableRows: List[(RowKey, List[String])],
      onRowClick: RowKey => Option[A],
      selectedRow: Option[RowKey]
  )(implicit dsl: ff4s.Dsl[F, S, A]): dsl.V = {

    import dsl._
    import dsl.html._

    val headerCls = "px-2 sticky text-center top-0 z-20 bg-green-500"
    val cellCls = "cursor-pointer px-2 text-center"
    val colsCls = s"grid-cols-${colNames.size}"

    div(
      cls := s"grid w-full h-full overflow-y-scroll auto-rows-min $colsCls rounded border",
      // table header
      colNames.map(name => div(cls := headerCls, name)),
      // table body
      tableRows.map { case (key, row) =>
        div(
          cls := "contents group/row",
          row.map(entry =>
            div(
              cls := cellCls ++ s" ${if (selectedRow.exists(_ === key)) "bg-red-500 group-hover/row:bg-red-200"
                else "group-hover/row:bg-red-200"}",
              onClick := (_ => onRowClick(key)),
              entry
            )
          )
        )
      }
    )

  }

}

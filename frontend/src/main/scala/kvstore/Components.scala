package kvstore

import org.scalajs.dom

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
        cls := "uppercase border rounded bg-green-500",
        label0,
        disabled := isDisabled(state),
        onClick := (_ => onClick0(state))
      )
    }
  }

  // custom text
  def customText(
      input0: String,
      placeholder0: String,
      onInput0: (S, String) => Option[A]
  )(implicit
      dsl: ff4s.Dsl[F, S, A]
  ): dsl.V = {
    import dsl._
    import dsl.html._

    useState { state =>
      textArea(
        cls := "border rounded",
        placeholder := placeholder0,
        onInput := ((ev: dom.Event) =>
          ev.target match {
            case el: dom.HTMLTextAreaElement => onInput0(state, el.value)
            case _                         => None
          }
        ),
        input0
      )
    }
  }

}

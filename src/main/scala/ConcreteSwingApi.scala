import scala.swing.event.Event

trait ConcreteSwingApi extends SwingApi {
  type ValueChanged = scala.swing.event.ValueChanged
  object ValueChanged {
    def unapply(x: Event) = x match {
      case vc: ValueChanged => Some(vc.source.asInstanceOf[TextField])
      case _ => None
    }
  }
  type ButtonClicked = scala.swing.event.ButtonClicked
  object ButtonClicked {
    def unapply(x: Event) = x match {
      case bc: ButtonClicked => Some(bc.source.asInstanceOf[Button])
      case _ => None
    }
  }

  type StateChanged = scala.swing.event.ButtonClicked
  object StateChanged {
    def unapply(x: Event) = x match {
      case sc: StateChanged => Some(sc.source.asInstanceOf[CheckBox])
      case _ => None
    }
  }

  type TextField = scala.swing.TextField
  type Button = scala.swing.Button
  type CheckBox = scala.swing.CheckBox
}


import scala.language.reflectiveCalls
import scala.swing.{CheckBox, Button}
import scala.swing.Reactions.Reaction
import scala.swing.event.{ButtonClicked, Event}
import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscription

trait SwingApi {

  def swing(body: =>Unit) = {
    val r = new Runnable { def run() = body }
    javax.swing.SwingUtilities.invokeLater(r)
  }


  implicit class ButtonOps(val self: Button) {
    def clicks = Observable.create[Unit] { obs =>
      self.reactions += {
        case ButtonClicked(_) => obs.onNext(())
      }
      Subscription()
    }
  }

  implicit class CheckBoxOps(val self: CheckBox) {
    def stateValues = Observable.create[Boolean] { obs =>
      self.reactions += {
        case ButtonClicked(x) => obs.onNext(x.selected)
      }
      Subscription()
    }
  }

}

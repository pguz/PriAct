import scala.language.reflectiveCalls
import scala.swing.Reactions.Reaction
import scala.swing.event.Event
import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscription

/** Basic facilities for dealing with Swing-like components.
*
* Instead of committing to a particular widget implementation
* functionality has been factored out here to deal only with
* abstract types like `ValueChanged` or `TextField`.
* Extractors for abstract events like `ValueChanged` have also
* been factored out into corresponding abstract `val`s.
*/
trait SwingApi {

  type ValueChanged <: Event

  val ValueChanged: {
    def unapply(x: Event): Option[TextField]
  }

  type ButtonClicked <: Event

  val ButtonClicked: {
    def unapply(x: Event): Option[Button]
  }

  type StateChanged <: Event

  val StateChanged: {
    def unapply(x: Event): Option[CheckBox]
  }

  type TextField <: {
    def text: String
    def subscribe(r: Reaction): Unit
    def unsubscribe(r: Reaction): Unit
  }

  type Button <: {
    def subscribe(r: Reaction): Unit
    def unsubscribe(r: Reaction): Unit
  }

  type CheckBox <: {
    def selected: Boolean
    def subscribe(r: Reaction): Unit
    def unsubscribe(r: Reaction): Unit
  }

  implicit class TextFieldOps(field: TextField) {

    def textValues: Observable[String] = 
      Observable.create(
        (observer: Observer[String]) => {
          val reactor: Reaction 
            = {case ValueChanged(x) => observer.onNext(field.text)}
          field.subscribe(reactor)
          new Subscription {
            override def unsubscribe: Unit = 
               field.unsubscribe(reactor)
          } 
        }
      )

  }

  implicit class ButtonOps(button: Button) {

    def clicks: Observable[Button] = 
      Observable.create(
        (observer: Observer[Button]) => {
          val reactor: Reaction 
            = {case ButtonClicked(_) => observer.onNext(button)}
          button.subscribe(reactor)
          new Subscription {
            override def unsubscribe: Unit = 
               button.unsubscribe(reactor)
          } 
        }
      )
  }

  implicit class CheckBoxOps(checkbox: CheckBox) {

    def stateValues: Observable[Boolean] =
      Observable.create(
        (observer: Observer[Boolean]) => {
          println("Selected")
          val reactor: Reaction
          = {
              case StateChanged(x) =>
                observer.onNext(x.selected)
          }

          checkbox.subscribe(reactor)
          new Subscription {
            override def unsubscribe: Unit =
              checkbox.unsubscribe(reactor)
          }
        }
      )
  }

}

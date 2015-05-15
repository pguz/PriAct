import actor.DispatcherActor.GetPrices
import actor.{DispatcherActor}
import akka.actor.{Props, ActorSystem}

object Main extends App {

  val system = ActorSystem("PriAct")

  //TODO: wyodrebnic hierarchie aktorow
  //stworzyc nadrzednego do komunikacji z uzytkownikiem
  val myActor = system.actorOf(Props[DispatcherActor], "dispatcher")
  print("Please, give product name: ")
  io.Source.stdin.getLines.foreach(prod => myActor ! GetPrices(prod))
}

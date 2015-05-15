package actor

import akka.actor.{Actor, Props}
import akka.event.Logging

object DispatcherActor {
  def props(): Props = Props(new DispatcherActor())

  sealed trait DispatcherRequest
  case class GetPrices(req: String) extends DispatcherRequest

  sealed trait DispatcherResponse
  case class Prices(prices: List[String]) extends DispatcherResponse
}

class DispatcherActor extends Actor {
  import DispatcherActor._
  val log = Logging(context.system, this)
  var gumtreeActor = context.actorOf(Props[GumtreeActor], name = "gumtree")
  var allegroActor = context.actorOf(Props[AllegroActor], name = "allegro")

  override def receive: Receive = {
    case GetPrices(product) => log.info(s"GetPrices: $product")
      gumtreeActor ! GumtreeActor.GetPrices(product)
      allegroActor ! AllegroActor.GetPrices(product)
    case GumtreeActor.Prices(prices) => log.info(s"Received prices from Gumtree")
      println("Gumtree: " + prices.toString()) //TODO: przeslac do aktora komunikujacego sie z uzytkownikiem
    case AllegroActor.Prices(prices) => log.info(s"Received prices from Allegro")
      println("Allegro: " + prices.toString()) //TODO: przeslac do aktora komunikujacego sie z uzytkownikiem
  }
}

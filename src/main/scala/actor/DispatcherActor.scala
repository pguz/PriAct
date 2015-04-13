package actor

import akka.actor.{Actor, Props}
import akka.event.Logging

object DispatcherActor {
  def props(): Props = Props(new DispatcherActor())

  sealed trait DispatcherRequest
  case class GetPrizes(req: String) extends DispatcherRequest

  sealed trait DispatcherResponse
  case class Prices(prices: List[String]) extends DispatcherResponse
}

class DispatcherActor extends Actor {
  import DispatcherActor._
  val log = Logging(context.system, this)
  var gumtreeActor = context.actorOf(Props[GumtreeActor], name = "gumtree")

  override def receive: Receive = {
    case GetPrizes(product) => log.info(s"GetPrizes: $product")
      gumtreeActor ! GumtreeActor.GetPrices(product)
    case GumtreeActor.Prices(prices) => log.info(s"Received prices")
      println(prices.toString()) //TODO: przeslac do aktora komunikujacego sie z uzytkownikiem
  }
}

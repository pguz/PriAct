package actor.mining

import actor.mining.AllegroActor.AllegroPrices
import actor.processing.PriceProcessingActor
import akka.actor.{Actor, Props}
import akka.event.Logging

object DispatcherActor {
  def props(): Props = Props(new DispatcherActor())

  sealed trait DispatcherRequest
  case class GetPrices(req: String) extends DispatcherRequest

}

class DispatcherActor extends Actor {
  import DispatcherActor._
  val log = Logging(context.system, this)
  var gumtreeActor = context.actorOf(Props[GumtreeActor], name = "gumtree")
  var allegroActor = context.actorOf(Props[AllegroActor], name = "allegro")
  var processingActor = context.actorOf(Props[PriceProcessingActor], name = "processing")

  override def receive: Receive = {
    case GetPrices(product) => log.info(s"GetPrices: $product")
      gumtreeActor ! GumtreeActor.GetPrices(product)
      allegroActor ! AllegroActor.GetPrices(product)
    case prices: Prices => log.info("Received prices")
      processingActor ! PriceProcessingActor.Process(prices)
//    case GumtreeActor.GumtreePrices(prices) => log.info(s"Received prices from Gumtree")
//      println("Gumtree: " + prices.toString()) //TODO: przeslac do aktora komunikujacego sie z uzytkownikiem
//    case AllegroActor.AllegroPrices(prices) => log.info(s"Received prices from Allegro")
//      println("Allegro: " + prices.toString()) //TODO: przeslac do aktora komunikujacego sie z uzytkownikiem
//      println("MIN: " + prices.min)
//      println("MAX: " + prices.max)
//      println("AVG: " + AllegroActor.AllegroPrices(prices).average)
  }
}

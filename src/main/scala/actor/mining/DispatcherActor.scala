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
  var olxActor = context.actorOf(Props[OlxActor], name = "olx")

  override def receive: Receive = {
    case GetPrices(product) => log.info(s"GetPrices: $product")
      gumtreeActor ! GumtreeActor.GetPrices(product)
      allegroActor ! AllegroActor.GetPrices(product)
    case prices: Prices => log.info("Received prices")
      processingActor ! PriceProcessingActor.Process(prices)
      
  }
}

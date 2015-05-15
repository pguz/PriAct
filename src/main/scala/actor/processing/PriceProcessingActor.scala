package actor.processing

import actor.mining.AllegroActor.AllegroPrices
import actor.mining.GumtreeActor.GumtreePrices
import actor.mining.Prices
import akka.actor.Actor
import akka.event.Logging

/**
 * Created by klis on 15.05.15.
 */
object PriceProcessingActor {
  case class Process(prices: Prices)
}
class PriceProcessingActor extends Actor {
  import PriceProcessingActor._
  val log = Logging(context.system, this)
  override def receive: Receive = {
    case Process(prices) => prices match {
      case AllegroPrices(prices) => log.info("Processing prices from Allegro")
        println("Processing Allegro prices!")
        println("MIN: " + prices.min)
        println("MAX: " + prices.max)
        println("AVG: " + AllegroPrices(prices).average)
      case GumtreePrices(prices) => log.info("Processing prices from Gumtree")
        println("Processing Gumtree prices!")
        println("MIN: " + prices.min)
        println("MAX: " + prices.max)
        println("AVG: " + AllegroPrices(prices).average)
    }

  }
}

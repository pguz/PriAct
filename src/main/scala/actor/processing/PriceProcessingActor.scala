package actor.processing

import java.sql.Timestamp

import actor.db.DBActor
import actor.mining.AllegroActor.AllegroPrices
import actor.mining.GumtreeActor.GumtreePrices
import actor.mining.OlxActor.OlxPrices
import actor.mining.Prices
import akka.actor.{Props, Actor}
import akka.event.Logging

/**
 * Created by klis on 15.05.15.
 */
object PriceProcessingActor {
  def props(): Props = Props(new PriceProcessingActor())

  case class Process(prices: Prices)
  case class ProcessedPrices(processedPrices: List[(String, String, String, Double)])
}
class PriceProcessingActor extends Actor {
  import PriceProcessingActor._
  val log = Logging(context.system, this)
  var dbActor = context.actorOf(Props[DBActor], name = "dbactor")
  override def receive: Receive = {
    case Process(prices) => prices match {
      case AllegroPrices(prices) => log.info("Processing prices from Allegro")
        println("Processing Allegro prices!")
        println("MIN: " + prices.min)
        println("MAX: " + prices.max)
        println("AVG: " + AllegroPrices(prices).average)
        dbActor ! DBActor.InsertPrices(ProcessedPrices(prices.map(p => ("a", "a", "a", p))))
      case GumtreePrices(prices) => log.info("Processing prices from Gumtree")
        println("Processing Gumtree prices!")
        println("MIN: " + prices.min)
        println("MAX: " + prices.max)
        println("AVG: " + GumtreePrices(prices).average)
      case OlxPrices(prices) => log.info("Processing prices from Olx")
        println("Processing Olx prices!")
        println("MIN: " + prices.min)
        println("MAX: " + prices.max)
        println("AVG: " + OlxPrices(prices).average)
    }

  }
}

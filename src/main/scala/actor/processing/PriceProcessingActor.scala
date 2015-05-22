package actor.processing

/**
 * Created by klis on 15.05.15.
 */

import actor.db.DBActor
import actor.mining.{CrawlerProtocol}
import akka.actor.{Props, Actor}
import akka.event.Logging

object ProcessingProtocol {
  sealed trait ProcessingResponse
  case class SendPrices()
}
class PriceProcessingActor extends Actor {
  val log = Logging(context.system, this)
  var dbActor = context.actorOf(Props[DBActor], name = "dbactor")
  override def receive: Receive = {
    case CrawlerProtocol.SendPrices(prices) => log.info("Processing prices from Processing")
      println("Processing Crawler prices!")
      println("MIN: " + prices.min)
      println("MAX: " + prices.max)

  }
}

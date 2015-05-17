package actor.processing

import actor.mining.{CrawlerActor}
import akka.actor.Actor
import akka.event.Logging

class PriceProcessingActor extends Actor {
  val log = Logging(context.system, this)
  override def receive: Receive = {
    case CrawlerActor.SendPrices(prices) => log.info("Processing prices from Processing")
      println("Processing Crawler prices!")
      println("MIN: " + prices.min)
      println("MAX: " + prices.max)
  }
}

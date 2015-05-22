package actor.db

/**
 * Created by klis on 15.05.15.
 */

import java.sql.Timestamp
import java.util.Calendar

import actor.mining.CrawlerProtocol
import actor.processing.PriceProcessingActor
import akka.actor.Actor
import akka.actor.Actor.Receive
import akka.event.Logging

import scala.slick.driver.H2Driver.simple._

class DBActor extends Actor{
  val log = Logging(context.system, this)

  val price: TableQuery[Price] = TableQuery[Price]
  val request: TableQuery[Request] = TableQuery[Request]
  val product: TableQuery[Product] = TableQuery[Product]

  val db = Database.forURL("jdbc:h2:pricesDB", driver = "org.h2.Driver")

  type PriceDetails = (String, String, String, Double)
  override def receive: Receive = {
    case CrawlerProtocol.SendPrices(prices) => log.debug("Received prices: " + prices.toString())
      prices.foreach( e =>
        product += (1, e._2, e._1))
      product foreach { case (id, name, url) =>
      println("  " + id + "\t" + name + "\t" + url)}
  }
}

//url: String, name: String, value: Double)
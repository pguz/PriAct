package actor.db

/**
 * Created by klis on 15.05.15.
 */

import java.sql.Timestamp
import java.util.Calendar

import actor.db.DBActor.InsertPrices
import actor.processing.PriceProcessingActor
import actor.processing.PriceProcessingActor.ProcessedPrices
import akka.actor.Actor
import akka.actor.Actor.Receive

import scala.slick.driver.H2Driver.simple._
object DBActor{
  case class InsertPrices(ps: ProcessedPrices)
}
class DBActor extends Actor{

  // The query interface for the Suppliers table
  val prices: TableQuery[PricesData] = TableQuery[PricesData]

  val db = Database.forURL("jdbc:h2:pricesDB", driver = "org.h2.Driver")

  type PriceDetails = (String, String, String, Double)
  override def receive: Receive = {
    case InsertPrices(ps) =>
      println("Inserting into DB!")
      db.withSession { implicit session => prices.ddl.create
        ps.processedPrices.foreach((p: PriceDetails) =>
          prices += (1, "3", "4", 5, new Timestamp(System.currentTimeMillis())))
      }
  }
}

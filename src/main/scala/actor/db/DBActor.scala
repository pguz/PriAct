package actor.db

/**
 * Created by klis on 15.05.15.
 */

import java.sql.Timestamp
import java.util.Calendar

import actor.mining.{DispatcherProtocol, CrawlerProtocol}
import akka.actor.Actor
import akka.actor.Actor.Receive
import akka.event.Logging

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

class DBActor extends Actor{
  val log = Logging(context.system, this)

  val price: TableQuery[Price] = TableQuery[Price]
  val request: TableQuery[Request] = TableQuery[Request]
  val product: TableQuery[Product] = TableQuery[Product]


  val db = Database.forURL("jdbc:h2:pricesDB", driver = "org.h2.Driver")

  def createIfNotExists(tables: TableQuery[_ <: Table[_]]*)(implicit session: Session) {
    tables foreach {table => if(MTable.getTables(table.baseTableRow.tableName).list.isEmpty) table.ddl.create}
  }

  db.withSession { implicit  session =>
    createIfNotExists(request, product, price)
  }

  type PriceDetails = (String, String, String, Double)
  override def receive: Receive = {
    case DispatcherProtocol.SendListPrices(prices) => log.debug("Received prices!")
      db.withSession { implicit session =>
        prices.foreach(e1 =>
          e1.info.foreach(e => {
            val prodId = (product returning product.map(_.id)) += (0, e._2, e._1)
//            val reqId = (request returning request.map(_.id)) +=
            price += (1, prodId, e._3)
          }
          ))
        product foreach { case (id, name, url) =>
          println("  " + id + "\t" + name + "\t" + url)
        }
        price foreach { case (reqId, prodId, value) =>
          println("  " + reqId + "\t" + prodId + "\t" + value)
        }
      }
      log.debug("Insert into DB complete!")
  }
}

//url: String, name: String, value: Double)
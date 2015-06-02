package actor.db

/**
 * Created by klis on 15.05.15.
 */

import java.sql.Timestamp

import actor.mining.DispatcherProtocol
import akka.actor.Actor
import akka.event.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

class DBActor extends Actor{
  val log = Logging(context.system, this)

  val tPrices: TableQuery[Prices] = TableQuery[Prices]
  val tQueries: TableQuery[Queries] = TableQuery[Queries]
  val tProducts: TableQuery[Products] = TableQuery[Products]


  val db = Database.forURL("jdbc:h2:file:pricesDB", driver = "org.h2.Driver")

  def createIfNotExists(tables: TableQuery[_ <: Table[_]]*)(implicit session: Session) {
    tables foreach {table => if(MTable.getTables(table.baseTableRow.tableName).list.isEmpty) table.ddl.create}
  }

  db.withSession { implicit  session =>
    createIfNotExists(tQueries, tProducts, tPrices)
  }

  override def receive: Receive = {
    case DispatcherProtocol.SendRequestContentAndPrices(req, prices) => log.debug("Received prices!")
      log.debug(context.dispatcher.toString)
      db.withSession { implicit session =>
        val reqId = (tQueries returning tQueries.map(_.id)) +=(0, req, new Timestamp(System.currentTimeMillis()))
        prices.foreach(e1 =>
          e1.info.foreach(e => {
            var prodId: Int = 0
            if (!tProducts.filter(_.url === e._1).exists.run) {
              prodId = (tProducts returning tProducts.map(_.id)) +=(0, e._2, e._1)
            }
            else {
              prodId = tProducts.filter(_.url === e._1).firstOption.get._1
            }
            tPrices +=(reqId, prodId, e._3)
          }
          ))
        //product foreach { case (id, name, url) =>
        //  println("  " + id + "\t" + name + "\t" + url)
        //}
        //price foreach { case (reqId, prodId, value) =>
        //  println("  " + reqId + "\t" + prodId + "\t" + value)
        //}
      log.debug("Insert into DB complete!")
    }
  }
}

//url: String, name: String, value: Double)
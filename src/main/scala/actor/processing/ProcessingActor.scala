package actor.processing

/**
 * Created by klis on 15.05.15.
 */

import actor.db.{Product, Request, Price, DBActor}
import actor.mining.{CrawlerProtocol}
import actor.processing.ProcessingProtocol.ReturnRequestsList
import akka.actor.Actor.Receive
import akka.actor.{Props, Actor}
import akka.event.Logging

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

object ProcessingProtocol {
  sealed trait ProcessingRequest
  case class ReturnRequestsList(content: String)
    extends ProcessingRequest
  case class ReturnRequestResult(requestId: Int)
    extends ProcessingRequest
  case class RequestStats(requestId: Int)

  sealed trait ProcessingResponse
  case class RequestsList(requstsIds: List[Int])
}
class PriceProcessingActor extends Actor {
  import ProcessingProtocol._
  val log = Logging(context.system, this)

  val priceT: TableQuery[Price] = TableQuery[Price]
  val requestsT: TableQuery[Request] = TableQuery[Request]
  val productT: TableQuery[Product] = TableQuery[Product]

  val tables = Seq(priceT, requestsT, productT)

  val db = Database.forURL("jdbc:h2:file:pricesDB", driver = "org.h2.Driver")

  def checkIfTablesExist(tables: Seq[TableQuery[_ <: Table[_]]])(implicit session: Session) {
    tables foreach {table => if(MTable.getTables(table.baseTableRow.tableName).list.isEmpty) throw new SlickException(s"Table $table doesn't exist. " +
      s"Create it before trying to access it.")}
  }

  checkIfTablesExist(tables)

  override def receive: Receive = {
    case ReturnRequestsList(requestContent) =>
      returnRequestsList(requestContent)
  }

  def returnRequestsList(requestContent: String) = { log.debug(s"Returning requests list for $requestContent")
    val requestsList = for {
      req <- requestsT if req.content like s"%$requestContent%"
    } yield (req.id)

    sender ! RequestsList(requestsList.list)
  }
}

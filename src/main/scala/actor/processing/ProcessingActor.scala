package actor.processing

/**
 * Created by klis on 15.05.15.
 */

import java.sql.Timestamp

import actor.db.{Products, Queries, Prices}
import akka.actor.{Actor}
import akka.event.Logging

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

object ProcessingProtocol {
  sealed trait ProcessingRequest
  case class RequestQueriesList(content: String)
    extends ProcessingRequest
  case class RequestQueryResult(queryId: Int)
    extends ProcessingRequest
  case class RequestQueryStats(queryId: Int)
    extends ProcessingRequest
  case class RequestProductStats(prodId: Int)
    extends ProcessingRequest


  sealed trait ProcessingResponse
  case class QueriesList(queriesIds: List[(Int, String, Timestamp)])
    extends ProcessingResponse
  case class QueryResult(prices: List[(Int, Double)]) //prodId, price
    extends ProcessingResponse
  case class QueryStats(min: Double, max: Double, avg: Double)
    extends ProcessingResponse
  case class ProductStats(min: Double, max: Double, avg: Double)
    extends ProcessingResponse
}
class PriceProcessingActor extends Actor {
  import ProcessingProtocol._
  val log = Logging(context.system, this)

  val tPrices: TableQuery[Prices] = TableQuery[Prices]
  val tQueries: TableQuery[Queries] = TableQuery[Queries]
  val tProducts: TableQuery[Products] = TableQuery[Products]

  val tables = Seq(tPrices, tQueries, tProducts)

  val db = Database.forURL("jdbc:h2:file:pricesDB", driver = "org.h2.Driver")

  def checkIfTablesExist(tables: Seq[TableQuery[_ <: Table[_]]])(implicit session: Session) {
    tables foreach {table => if(MTable.getTables(table.baseTableRow.tableName).list.isEmpty) throw new SlickException(s"Table $table doesn't exist. " +
      s"Create it before trying to access it.")}
  }

  db.withSession {
    implicit session => checkIfTablesExist(tables)
  }


  override def receive: Receive = {
    case RequestQueriesList(requestContent) =>
      returnRequestsList(requestContent)
    case RequestQueryResult(queryId) =>
      returnQueryResult(queryId)
    case RequestQueryStats(queryId) =>
      returnQueryStats(queryId)
    case RequestProductStats(prodId) =>
      returnProductStats(prodId)

  }

  def returnRequestsList(requestContent: String) = { log.debug(s"Returning requests list for $requestContent")
    db.withSession { implicit session =>
      val requestsList = for {
        req <- tQueries if req.content like s"%$requestContent%"
      } yield (req.id, req.content, req.date)

      sender ! QueriesList(requestsList.list)
    }
  }

  def returnQueryResult(queryId: Int) = { log.debug(s"Returning result for query: $queryId")
    db.withSession { implicit session =>
      val pricesList = for {
        price <- tPrices if price.queryId === queryId
      } yield (price.prodId, price.value)
      println("pricesList.list: " + pricesList.list)
      sender ! QueryResult(pricesList.list)
    }
  }

  def returnQueryStats(queryId: Int) = { log.debug(s"Returning stats for query: $queryId")
    val queryResult = db.withSession { implicit session =>
      val pricesList = for {
        price <- tPrices if price.queryId === queryId
      } yield (price.prodId, price.value)
      pricesList.list
    }
    sender ! QueryStats(queryResult.minBy(_._2)._2, queryResult.maxBy(_._2)._2, BigDecimal(queryResult.foldLeft(0.0)((r, c) => r + c._2)/queryResult.length).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
  }

  def returnProductStats(prodId: Int) = { log.debug(s"Returning result for product: $prodId")
    val queryResult = db.withSession { implicit session =>
      val pricesList = for {
        price <- tPrices if price.prodId === prodId
      } yield (price.prodId, price.value)
      pricesList.list
    }
    sender ! ProductStats(queryResult.minBy(_._2)._2, queryResult.maxBy(_._2)._2, BigDecimal(queryResult.foldLeft(0.0)((r, c) => r + c._2)/queryResult.length).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)

  }
}

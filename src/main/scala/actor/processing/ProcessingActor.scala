package actor.processing

/**
 * Created by klis on 15.05.15.
 */

import java.sql.Timestamp

import actor.db.{Products, Queries, Prices, DBActor}
import actor.mining.{CrawlerProtocol}
import actor.processing.ProcessingProtocol.RequestQueriesList
import akka.actor.Actor.Receive
import akka.actor.{Props, Actor}
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
  case class RequestProductPrices(productURL: String)
    extends ProcessingRequest


  sealed trait ProcessingResponse
  case class QueriesList(queriesIds: List[Int])
    extends ProcessingResponse
  case class QueryResult(prices: List[(Int, Double)]) //prodId + price
    extends ProcessingResponse
  case class QueryStats(min: Double, max: Double, avg: Double)
    extends ProcessingResponse
  case class ProductPrices(prices: List[(Double, Timestamp)])
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
    case RequestProductPrices(productURL) =>
      returnProductPrices(productURL)

  }

  def returnRequestsList(requestContent: String) = { log.debug(s"Returning requests list for $requestContent")
    db.withSession { implicit session =>
      val requestsList = for {
        req <- tQueries if req.content like s"%$requestContent%"
      } yield (req.id)

      sender ! QueriesList(requestsList.list)
    }
  }

  def returnQueryResult(queryId: Int) = { log.debug(s"Returning result for query: $queryId")
    db.withSession { implicit session =>
      val pricesList = for {
        price <- tPrices if price.queryId === queryId
      } yield (price.prodId, price.value)

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
    sender ! QueryStats(queryResult.minBy(_._2)._2, queryResult.maxBy(_._2)._2, queryResult.foldLeft(0.0)((r, c) => r + c._2)/queryResult.length)
  }

  def returnProductPrices(productURL: String) = { log.debug(s"Returning result for product: $productURL")
    db.withSession { implicit session =>
      val pricesList = for {
        product <- tProducts if product.url === productURL
        price <- tPrices if price.prodId === product.id
        query <- tQueries if query.id === price.queryId
      } yield (price.value, query.date)

      sender ! ProductPrices(pricesList.list)
    }

  }
}

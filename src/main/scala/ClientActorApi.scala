import java.sql.Timestamp

import actor.mining.DispatcherActor
import actor.processing.{ProcessingProtocol, PriceProcessingActor}
import actor.processing.ProcessingProtocol.ProcessingRequest

import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.config.ConfigFactory

trait ClientActorApi {
  import actor.mining.DispatcherProtocol._

  implicit val timeout: Timeout = Timeout(120 seconds)
  val system = ActorSystem("PriAct", akkaConfig)
  val dispatcherActor = system.actorOf(Props(classOf[DispatcherActor]), "dispatcher")
  val processingActor = system.actorOf(Props(classOf[PriceProcessingActor]), "processing")

  //przeniesc do configu
  def akkaConfig = ConfigFactory.parseString(s"""
    akka {
      loglevel = "DEBUG"
      actor.deployment {
        /dbactor {
          dispatcher = dbdispatcher
        }
      }
    }
    dbdispatcher {
      type = PinnedDispatcher
      executor = "thread-pool-executor"
    }
  """)

  def createCrawler(name: String): Future[Boolean]  = {
    val f = dispatcherActor ? CreateCrawler(name)
    f.mapTo[DispatcherResponse].map(_ match {
      case CrawlerAdded     => true
      case CrawlerNotFound  => false
    })
  }

  def removeCrawler(name: String): Future[Boolean]  = {
    val f = dispatcherActor ? RemoveCrawler(name)
    f.mapTo[DispatcherResponse].map(_ match {
      case CrawlerRemoved   => true
      case CrawlerNotFound  => false
    })
  }

  def getPrices(product: String): Future[List[(String, String, String, Double)]] = {
    val f = dispatcherActor ? GetPrices(product)
    f.mapTo[SendListPrices].map{case SendListPrices(x)
      => x.flatMap(y => y.info.map{case (id, name, price) => (y.crawName, id, name, price)})}
  }

  def getDescription(shop: String, id: String): Future[String] = {
    val f = dispatcherActor ? GetDescription(shop, id)
    f.mapTo[SendDescription].map{case SendDescription(desc) => desc}
  }

  def fRequestQuriesList(content: String): Future[List[(Int, String, Timestamp)]] = {
    val f = processingActor ? ProcessingProtocol.RequestQueriesList(content)
    f.mapTo[ProcessingProtocol.QueriesList].map{case ProcessingProtocol.QueriesList(queriesIds) => queriesIds}
  }

  def fRequestQueryResult(queryId: Int): Future[List[(Int, Double)]] = {
    val f = processingActor ? ProcessingProtocol.RequestQueryResult(queryId)
    f.mapTo[ProcessingProtocol.QueryResult].map{case ProcessingProtocol.QueryResult(prices) => prices}
  }

  def fRequestQueryStats(queryId: Int): Future[(Double, Double, Double)] = {
    val f = processingActor ? ProcessingProtocol.RequestQueryStats(queryId)
    f.mapTo[ProcessingProtocol.QueryStats].map{case ProcessingProtocol.QueryStats(min, max, avg) => (min, max, avg)}
  }
}

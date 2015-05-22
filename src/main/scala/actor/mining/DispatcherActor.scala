package actor.mining

import actor.db.DBActor
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}


object DispatcherProtocol {
  
  sealed trait DispatcherRequest
  case class CreateCrawler(name: String)
    extends DispatcherRequest
  case class RemoveCrawler(name: String)
    extends DispatcherRequest
  case class GetPrices(req: String)
    extends DispatcherRequest
  case class GetDescription(shop: String, id: String)
    extends DispatcherRequest

  sealed trait DispatcherResponse
  case object NoSuchMessage
    extends DispatcherResponse
  case object CrawlerAdded
    extends DispatcherResponse
  case object CrawlerRemoved
    extends DispatcherResponse
  case object CrawlerNotFound
    extends DispatcherResponse
  case class SendPrices(crawName: String, info: List[(String, String, Double)])
    extends DispatcherResponse
  case class SendListPrices(prices: List[SendPrices])
    extends DispatcherResponse
  case class SendRequestContentAndPrices(request: String, prices: List[SendPrices])
    extends DispatcherResponse
  case class SendDescription(desc: String)
    extends DispatcherResponse
}

class DispatcherActor extends Actor with ActorLogging {
  import DispatcherProtocol._

  implicit val timeout: Timeout
    = Timeout(50 seconds)
  var dbActor
    = context.actorOf(Props[DBActor], name = "db")
  var crawList: List[CrawlerActorRef]
    = List()

  override def receive: Receive = {
    case CreateCrawler(crawName)
      => crawlerCreation(crawName)

    case RemoveCrawler(crawName)
      => crawlerRemoval(crawName)

    case GetPrices(prod)
      => savePrices(prod)

    case GetDescription(shop, id)
      => descProcess(shop, id)

    case _
      => sender() ! NoSuchMessage
  }

  def crawlerCreation(crawName: String): Unit = {
    log.info(s"CreateCrawler: $crawName")

    createCrawler(crawName) match {
      case Some(actorRef) => {
        crawList = actorRef :: crawList
        log.info(s"$crawName has been added")
        sender ! CrawlerAdded
      }
      case None => {
        log.info(s"$crawName has not been added")
        sender ! CrawlerNotFound
      }
    }
  }

  def crawlerRemoval(crawName: String): Unit = {
    log.info(s"RemoveCrawler: $crawName")

    removeCrawler(crawName) match {
      case true   =>
        log.debug(s"$crawName has been removed")
        sender ! CrawlerRemoved
      case false  =>
        log.debug(s"$crawName has not been removed")
        sender ! CrawlerNotFound
    }
  }

  def savePrices(prod: String): Unit = {
    log.info(s"GetPrices: $prod")

    val reqSender = sender // inaczej nie dziala, wspolbieznie moze byc niebezpiecznie. Ojoj :'(

    Future.sequence(crawList.map(crawler => getPrices(crawler, prod))) onComplete {
      case Success(x) => {
        log.debug("GetPrices with success")
        reqSender ! SendListPrices(x)
        dbActor ! SendRequestContentAndPrices(prod, x)
        log.debug("Sent prices to DB")
      }
      case Failure(err) => {
        log.debug("GetPrices with error: " + err.getMessage)
        reqSender ! Status.Failure(err)
      }
    }
  }

  def descProcess(shop: String, id: String): Unit = {
    log.info(s"GetDesc: $id from $shop")

    val reqSender = sender

    crawList.find(_.name == shop) match {
      case Some(crawler) => {
        (crawler.actorRef ? CrawlerProtocol.GetDescription(id)).mapTo[CrawlerProtocol.SendDescription] onComplete {
          case Success(CrawlerProtocol.SendDescription(d)) => {
            log.debug("GetDescription with success")
            reqSender ! SendDescription(d)
          }
          case Failure(err) => {
            log.debug("GetDescription with error: " + err.getMessage)
            reqSender ! Status.Failure(err)
          }
        }
      }
      case None =>
        sender() ! CrawlerNotFound
    }
  }

  def createCrawler(crawName: String): Option[CrawlerActorRef] = crawName match {
    case "Allegro"
      => Some(new AllegroActorRef(context.actorOf(Props[AllegroActor], name = crawName), crawName))
    case "Gumtree"
      => Some(new GumtreeActorRef(context.actorOf(Props[GumtreeActor], name = crawName), crawName))
    case "Olx"
      => Some(new OlxActorRef(context.actorOf(Props[OlxActor], name = crawName), crawName))
    case _
      => None
  }

  def removeCrawler(crawName: String): Boolean = {
    val crawsPart = crawList.partition(e => e.name == crawName)
    if(crawsPart._1.size == 0)
      return false
    crawList = crawsPart._2
    crawsPart._1.foreach(_.actorRef ! PoisonPill)
    return true
  }

  def getPrices(crawler: CrawlerActorRef, prod: String): Future[SendPrices] =
    (crawler.actorRef ? CrawlerProtocol.GetPrices(prod)).mapTo[CrawlerProtocol.SendPrices].map{
      case CrawlerProtocol.SendPrices(l) => SendPrices(crawler.name, l)}

}

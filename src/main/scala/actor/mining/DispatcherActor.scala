package actor.mining

import actor.processing.PriceProcessingActor
import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}


object DispatcherActor {
  
  sealed trait DispatcherRequest
  case class CreateCrawler(name: String) extends DispatcherRequest
  case class RemoveCrawler(name: String) extends DispatcherRequest
  case class GetPrices(req: String) extends DispatcherRequest

  sealed trait DispatcherResponse
  case object CrawlerAdded extends  DispatcherResponse
  case object CrawlerRemoved extends  DispatcherResponse
  case object CrawlerNotFound extends DispatcherResponse
  case class SendPrices(crawName: String, prices: List[Double])
  case class SendListPrices(prices: List[SendPrices])
}

class DispatcherActor extends Actor {
  import DispatcherActor._
  val log = Logging(context.system, this)
  implicit val timeout: Timeout = Timeout(50 seconds)
  var processingActor = context.actorOf(Props[PriceProcessingActor], name = "processing")

  var crawList: List[CrawlerActorRef] = List()

  def create(crawName: String): Option[CrawlerActorRef] = crawName match {
    case "Allegro"  => Some(new AllegroActorRef(context.actorOf(Props[AllegroActor], name = crawName), crawName))
    case "Gumtree"  => Some(new GumtreeActorRef(context.actorOf(Props[GumtreeActor], name = crawName), crawName))
    case "Olx"      => Some(new OlxActorRef(context.actorOf(Props[OlxActor], name = crawName), crawName))
    case _          => None
  }

  def remove(crawName: String): Boolean = {
    val crawsPart = crawList.partition(e => e.name == crawName)

    if(crawsPart._1.size == 0)
      return false

    crawList = crawsPart._2
    crawsPart._1.foreach(_.actorRef ! PoisonPill)
    return true
  }

  def getPrices(crawler: CrawlerActorRef, prod: String): Future[DispatcherActor.SendPrices] =
    (crawler.actorRef ? CrawlerActor.GetPrices(prod)).mapTo[CrawlerActor.SendPrices].map{
      case CrawlerActor.SendPrices(l) => DispatcherActor.SendPrices(crawler.name, l)}

  override def receive: Receive = {
    case CreateCrawler(crawName) => {
      println("CreateCrawler: " + crawName)

      create(crawName) match {
        case Some(actorRef) => {
          crawList = actorRef :: crawList
          sender ! CrawlerAdded
        }
        case None => {
          sender ! CrawlerNotFound
        }
      }
    }

    case RemoveCrawler(crawName) => {
      println("RemoveCrawler: " + crawName)

      remove(crawName) match {
        case true   =>
          sender ! CrawlerRemoved
        case false  =>
          sender ! CrawlerNotFound
      }
    }


    case GetPrices(prod) => {
      println(s"GetPrices: $prod")

      val reqSender = sender // inaczej nie dziala.

      Future.sequence(crawList.map(crawler => getPrices(crawler, prod))) onComplete {
        case Success(x) => {
          println("GetPrices Success " + x)
          reqSender ! DispatcherActor.SendListPrices(x)
        }
        case Failure(err) => {
          println("GetPrices Failure " + err.getMessage)
          reqSender ! Status.Failure(err)
        }
      }
    }
  }
}

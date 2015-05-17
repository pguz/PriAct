package actor.mining

import actor.processing.PriceProcessingActor
import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}


object DispatcherActor {
  
  sealed trait DispatcherRequest
  case class CreateCrawler(name: String) extends DispatcherRequest
  case class GetPrices(req: String) extends DispatcherRequest

  sealed trait DispatcherRespone
  case object CrawlerAdded extends  DispatcherRespone
  case object CrawlerNotFound extends DispatcherRespone
  case class SendPrices(crawName: String, prices: List[Double])
}

class DispatcherActor extends Actor {
  import DispatcherActor._
  val log = Logging(context.system, this)
  implicit val timeout: Timeout = Timeout(16 seconds)
  var processingActor = context.actorOf(Props[PriceProcessingActor], name = "processing")

  var crawList: List[CrawlerActorRef] = List()

  def create(crawName: String): Option[CrawlerActorRef] = crawName match {
    case "Allegro"  => Some(new AllegroActorRef(context.actorOf(Props[AllegroActor], name = crawName), crawName))
    //case "gumtree"  => Some(Props[GumtreeActor])
    //case "olx"      => Some(Props[OlxActor])
    case _          => None

  }

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

    case GetPrices(prod) => {
      println(s"GetPrices: $prod")

      val reqSender = sender // inaczej nie dziala.
      crawList foreach {
        crawler =>
          (crawler.actorRef ? crawler.getPrices(prod)).mapTo[CrawlerActor.SendPrices] onComplete {
             case Success(CrawlerActor.SendPrices(prices)) => {
               println("GetPrices Success")
               reqSender ! DispatcherActor.SendPrices(crawler.name, prices)
             }
             case Failure(err) => {
               println("GetPrices Failure")
               reqSender ! Status.Failure(err)
             }
          }
      }
    }
  }
}

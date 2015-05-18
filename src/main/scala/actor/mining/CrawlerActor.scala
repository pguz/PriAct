package actor.mining

import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._

object CrawlerProtocol {
  sealed trait CrawlerRequest
  case class GetPrices(prod: String)
    extends CrawlerRequest
  case class GetDescription(id: String)
    extends CrawlerRequest
  case object ByeRequest
    extends CrawlerRequest

  sealed trait CrawlerResponse
  case object NoSuchMessage
    extends CrawlerResponse
  case class SendPrices(prices: List[Double])
    extends CrawlerResponse
  case class SendDescription(desc: String)
    extends CrawlerResponse
  case object ByeResponse
    extends CrawlerResponse
}

abstract class CrawlerActor extends Actor with ActorLogging {
  import CrawlerProtocol._

  implicit val timeout: Timeout = Timeout(16 seconds)

  override def receive: Receive = {
    case GetPrices(prod)
      => priceGetter(prod)
    case GetDescription(id)
      => descriptionGetter(id)
    case ByeRequest
      => bye()
    case _
      => sender() ! NoSuchMessage
  }

  def priceGetter(prod: String): Unit = {
    log.debug("GetPrices: " + prod)
    sender() ! SendPrices(getPrices(prod).map(_.toDouble))
  }

  def descriptionGetter(id: String): Unit = {
    log.debug("GetDesc: " + id)
    sender() ! SendDescription(getDescription(id))
  }

  def bye(): Unit = {
    sender() ! ByeResponse
    context.stop(self)
  }

  def getPrices(product: String): List[String]
  def getDescription(id: String): String
}

object CrawlerActorRef {
}

abstract class CrawlerActorRef(val actorRef: ActorRef, val name: String) {
}

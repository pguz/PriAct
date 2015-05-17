package actor.mining

import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._

object CrawlerActor {
  sealed trait CrawlerRequest
  case class GetPrices(prod: String) extends CrawlerRequest

  sealed trait CrawlerResponse
  case class SendPrices(prices: List[Double]) extends CrawlerResponse
}

abstract class CrawlerActor extends Actor {
  implicit val timeout: Timeout = Timeout(16 seconds)

  override def receive: Receive = {
    case CrawlerActor.GetPrices(product) => {
      println("CrawlerActor GetPrices: " + product)
      sender() ! CrawlerActor.SendPrices(getPrices(product).map(_.toDouble))
    }
    case _ =>
      println("CrawlerActor: No proper case class")
  }

  def getPrices(product: String): List[String]

}

object CrawlerActorRef {
}

abstract class CrawlerActorRef(val actorRef: ActorRef, val name: String) {
}

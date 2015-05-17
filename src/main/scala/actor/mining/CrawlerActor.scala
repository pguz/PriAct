package actor.mining

import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._

/**
 * Created by galvanize on 5/16/15.
 */

object CrawlerActor {
  sealed trait CrawlerRequest
  class GetPrices(prod: String) extends CrawlerRequest

  sealed trait CrawlerResponse
  case class SendPrices(prices: List[Double]) extends CrawlerResponse
}

abstract class CrawlerActor extends Actor {
  implicit val timeout: Timeout = Timeout(16 seconds)
}

object CrawlerActorRef {
}

abstract class CrawlerActorRef(val actorRef: ActorRef, val name: String) {
  def getPrices(prod: String): CrawlerActor.GetPrices
}

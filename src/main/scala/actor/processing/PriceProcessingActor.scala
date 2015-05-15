package actor.processing

import akka.actor.Actor
import akka.actor.Actor.Receive

/**
 * Created by klis on 15.05.15.
 */
object PriceProcessingActor {
  case class calculateAverage(prices: List[Double])
}
class PriceProcessingActor extends Actor {
  override def receive: Receive = ???
}

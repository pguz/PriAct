import actor.mining.DispatcherActor
import actor.mining.DispatcherActor._
import akka.actor.{Props, ActorSystem}
import akka.util.Timeout

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask

trait ClientActorApi {
  implicit val timeout: Timeout = Timeout(16 seconds)
  val system = ActorSystem("PriAct")
  val dispatcherActor = system.actorOf(Props(classOf[DispatcherActor]), "dispatcher")

  def createCrawler(name: String): Future[Boolean]  = {
    val f = dispatcherActor ? DispatcherActor.CreateCrawler(name)
    f.mapTo[DispatcherResponse].map(_ match {
      case CrawlerAdded     => true
      case CrawlerNotFound  => false
    })
  }

  def getPrices(product: String): Future[List[(String, Double)]] = {
    val f = dispatcherActor ? DispatcherActor.GetPrices(product)
    f.mapTo[SendListPrices].map{case SendListPrices(x) => x.flatMap(y => y.prices.map((y.crawName, _)))}
    //f.mapTo[List[DispatcherActor.SendPrices]].map(x => x.map(y => y.prices.map( (y.crawName, _) ) ) )
  }
}

package actor

import akka.actor.Actor
import akka.event.Logging
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.util.matching.Regex

/**
 * Created by galvanize on 4/13/15.
 */

object GumtreeActor {

  sealed trait GumtreeRequest
  case class GetPrices(req: String) extends GumtreeRequest

  sealed trait GumtreeResponse
  case class Prices(prices: List[String]) extends GumtreeResponse

  val prizePattern = new Regex("[0-9]+")
  val prizeClass = "ar-price"

  def getSourceCode(product: String): Document = {
    Jsoup.connect(s"http://www.gumtree.pl/fp-$product?Page=1").get()
  }
}


class GumtreeActor extends Actor {
  import actor.GumtreeActor._
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case GetPrices(product) => log.info(s"GetPrices: $product")
      sender() ! Prices(getPrices(product))
  }

  def getPrices(product: String) = {
    var list: List[String] = List()

    //malo funkcyjnie, wykorzystana javowa biblioteka Jsoup
    val doc = getSourceCode(product)
    val prices = doc.body().getElementsByClass(prizeClass)
    val price: java.util.Iterator[Element] = prices.iterator()
    while(price.hasNext()) {
      val cur_price = price.next()
      if(cur_price.children().size() > 0) {
        val ext_prize = prizePattern.findFirstIn(cur_price.child(0).text().replaceAll(" ", ""))
        if(!ext_prize.isEmpty) {
          list = ext_prize.get :: list
        }
      }
    }
    list.reverse
  }
}

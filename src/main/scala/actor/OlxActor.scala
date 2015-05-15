package actor

import akka.actor.Actor
import akka.event.Logging
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.util.matching.Regex

/**
 * Created by galvanize on 4/13/15.
 */

object OlxActor {

  sealed trait OlxRequest
  case class GetPrices(req: String) extends OlxRequest

  sealed trait OlxResponse
  case class Prices(prices: List[String]) extends OlxResponse

  val pricePattern = new Regex("[0-9]+")
  val priceClass = "price"

  def getSourceCode(product: String): Document = {
    Jsoup.connect(s"http://olx.pl/oferty/q-$product/").get()
  }
}


class OlxActor extends Actor {
  import actor.OlxActor._
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case GetPrices(product) => log.info(s"GetPrices: $product")
      sender() ! Prices(getPrices(product))
  }

  def getPrices(product: String) = {
    var list: List[String] = List()

    //malo funkcyjnie, wykorzystana javowa biblioteka Jsoup
    val doc = getSourceCode(product)
    val prices = doc.body().getElementsByClass(priceClass)
    val price: java.util.Iterator[Element] = prices.iterator()
    while(price.hasNext()) {
      val cur_price = price.next()
      if(cur_price.children().size() > 0) {
        val ext_price = pricePattern.findFirstIn(cur_price.child(0).text().replaceAll(" ", ""))
        if(!ext_price.isEmpty) {
          list = ext_price.get :: list
        }
      }
    }
    list.reverse
  }
}

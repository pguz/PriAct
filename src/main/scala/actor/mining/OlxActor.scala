package actor.mining

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
    // Olx parametry GETa
    // page=1 - numer strony z wynikami, jesli damy za duzy to przekieruje nas do http://olx.pl/oferty/?page=xx czyli wywali
    // nasze zapytanie
    // search[description]=1 - tak jak w allegro - szuka także w parametrach i opisach
    // search[order]=filter_float_price%3Aasc - sortuje od najtanszych
    // search[order]=filter_float_price%3Adesc - sortuje od najdrozszych
    // search[order]=created_at%3Adesc - sortuje od najnowszych
    // view=list -> widok listy; galleryWide - galeria 1; galleryBig - galeria typ 2
    // TODO: warunek stopu dla pętli pobierania - sprawdzenie czy to co dostajemy ma w adresie q-$product
    Jsoup.connect(s"http://olx.pl/oferty/q-$product/").get()
  }
}


class OlxActor extends Actor {
  import OlxActor._
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

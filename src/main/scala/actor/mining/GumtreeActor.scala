package actor.mining

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
  case class Prices(prices: List[Double]) extends GumtreeResponse

  val pricePattern = new Regex("[0-9]+")
  val priceClass = "ar-price"

  def getSourceCode(product: String): Document = {
    // Gumtree parametry GET'a
    // Sort=1 sortowanie -> 1 - sortuje po dacie od najstarszych; 2 - po dacie od najnowszych; 3 - po cenie od najnizszej; 4 - po cenie od najwyzszej
    // wieksze od 4 - po dacie od najnowszych
    // AdType=2 wybor rodzaju ogloszenie -> 1 - ogłoszenie "poszukuję"; 2 - "oferuję"; inne to ogłoszenia pracy itp
    // gallery=false -> false - widok listy; true - widok galerii
    // Page=1 numer strony z wynikami -> podanie wiekszego numeru strony niz ostatni (np. 4 gdzie mamy 1-3) powoduje wyswietlenie ostatniej strony wynikow (czyli np. 3)
    //TODO: warunek stopu pętli pobierania - sprawdzenie czy kolejna strona zwraca taką samą treść jak poprzednia
    Jsoup.connect(s"http://www.gumtree.pl/fp-$product?Page=1").get()
  }
}


class GumtreeActor extends Actor {
  val log = Logging(context.system, this)
  import GumtreeActor._
  override def receive: Receive = {
    case GetPrices(product) => log.info(s"GetPrices: $product")
      sender() ! Prices(getPrices(product).map((x:String) => x.toDouble))
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
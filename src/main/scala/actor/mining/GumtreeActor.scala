package actor.mining

import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.util.matching.Regex

object GumtreeActor {

  val pricePattern = new Regex("[0-9]+")
  val priceClass = "ar-price"

  def getSourceCode(product: String): Document = {
    // Gumtree parametry GET'a
    // Sort=1 sortowanie -> 1 - sortuje po dacie od najstarszych; 2 - po dacie od najnowszych; 3 - po cenie od najnizszej; 4 - po cenie od najwyzszej
    // wieksze od 4 - po dacie od najnowszych
    // AdType=2 wybor rodzaju ogloszenie -> 1 - ogłoszenie "poszukuję"; 2 - "oferuję"; inne to ogłoszenia pracy itp
    // gallery=false -> false - widok listy; true - widok galerii
    // Page=1 numer strony z wynikami -> podanie wiekszego numeru strony niz ostatni (np. 4 gdzie mamy 1-3) powoduje wyswietlenie ostatniej strony wynikow (czyli np. 3)
    //TODO: parsowanie tytułów bo gumtree sprawdza zarówno w opisie i tytule
    //TODO: warunek stopu pętli pobierania - sprawdzenie czy kolejna strona zwraca taką samą treść jak poprzednia
    Jsoup.connect(s"http://www.gumtree.pl/fp-$product?Page=1").get()
  }
}


class GumtreeActor extends CrawlerActor {
  val log = Logging(context.system, this)
  import GumtreeActor._

  override def getPrices(product: String): List[String] = {
    println("GumtreeActor: getPrices")
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

class GumtreeActorRef(override val actorRef: ActorRef, override val name: String)
    extends CrawlerActorRef(actorRef, name) {
}

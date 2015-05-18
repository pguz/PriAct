package actor.mining

import akka.actor.{ActorRef}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.util.matching.Regex
object OlxActor {

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


class OlxActor extends CrawlerActor {
  import OlxActor._

  override def getPrices(product: String): List[String] = {
    var list: List[String] = List()
    println("OlxActor: getPrices")
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
    list.reverse.map(_.replace(',','.'))
  }

  override def getDescription(id: String): String = s"Olx $id: MOCK"
}

class OlxActorRef(override val actorRef: ActorRef, override val name: String)
    extends CrawlerActorRef(actorRef, name) {
}

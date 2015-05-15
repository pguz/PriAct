package actor.mining

import akka.actor.Actor
import akka.event.Logging
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.util.matching.Regex

/**
 * Created by galvanize on 4/13/15.
 */

object AllegroActor {

  sealed trait AllegroRequest
  case class GetPrices(req: String) extends AllegroRequest

  sealed trait AllegroResponse
  case class Prices(prices: List[Double]) extends AllegroResponse

  val pricePattern = new Regex("""(\d+\s?)+(,\d{1,2})?""")
  val priceClass = "price"

  def getSourceCode(product: String): Document = {
    // Allegro parametry GET'a
    // description=1 -> wyszukuje także w opisach i parametrach
    // p=1 -> numer strony, jesli wyjdziemy wyzej to wyjdzie nam komunikat z pusta strona ("Wygląda na to, że nie mamy tego, czego szukasz.")
    // limit=180 -> limit ilosci wpisow na strone - dostepne 60, 120, 180, inna wartosc wyswietli 60
    // string=asd wyszukiwany tekst
    //TODO: warunek stopu dla pętli pobierania: sprawdzenie czy na stronie wynikowej mamy komunikat zawarty wyżej
    Jsoup.connect(s"http://allegro.pl/listing/listing.php?bmatch=seng-ps-mp-p-sm-isqm-2-e-0402&order=p&description=1&limit=180&string=$product").get()
  }
}


class AllegroActor extends Actor {
  val log = Logging(context.system, this)
  import AllegroActor._
  override def receive: Receive = {
    case GetPrices(product) => log.info(s"GetPrices: $product")
     sender() ! Prices(getPrices(product).map((x:String) => x.replace(',','.').toDouble))

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
        val ext_price = pricePattern.findFirstIn(cur_price.child(0).text())
        if(!ext_price.isEmpty) {
          list = ext_price.get :: list
        }
      }
    }
    list.reverse
  }
}

package actor.mining

import akka.actor.{ActorRef}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import scala.util.matching.Regex

object AllegroActor  {
  val pricePattern = new Regex("""\d+,\d{2}?""")
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


class AllegroActor extends CrawlerActor {
  import AllegroActor._

  override def getPrices(product: String): List[String]= {
    println("AllegroActor: getPrices")
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
    list.reverse.sorted.map(_.replace(',','.'))
  }

  override def getDescription(id: String): String = s"Allegro $id: MOCK"
}

class AllegroActorRef(override val actorRef: ActorRef, override val name: String)
    extends CrawlerActorRef(actorRef, name) {
}

package actor.mining

import akka.actor.ActorRef
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.util.matching.Regex

object AllegroActor  {
  val pricePattern = new Regex("""\d+,\d{2}?""")
  val priceClass = "price"
  val iterableClass = "excerpt"
  val descriptionId = "user_field"

  def getSourceCode(product: String, page: Integer): Document = {
    // Allegro parametry GET'a
    // description=1 -> wyszukuje także w opisach i parametrach
    // p=1 -> numer strony, jesli wyjdziemy wyzej to wyjdzie nam komunikat z pusta strona ("Wygląda na to, że nie mamy tego, czego szukasz.")
    // limit=180 -> limit ilosci wpisow na strone - dostepne 60, 120, 180, inna wartosc wyswietli 60
    // string=asd wyszukiwany tekst
    // offerTypeBuyNow=1 -> tylko kup teraz
    //warunek stopu dla pętli pobierania: sprawdzenie czy na stronie wynikowej mamy komunikat zawarty wyżej
    //println("AllegroActor: getSourceCode, search product " + product + ", page " + page)
    Jsoup.connect(s"http://allegro.pl/listing/listing.php?bmatch=seng-ps-mp-p-sm-isqm-2-e-0402&order=p&description=0&limit=180&string=$product&p=$page&offerTypeBuyNow=1").get()
  }
}


class AllegroActor extends CrawlerActor {
  import AllegroActor._
  import scala.util.control.Breaks._
  import scala.collection.mutable.ListBuffer

  override def getPrices(product: String): List[(String, String, Double)]= {
    println("AllegroActor: getPrices")
    val contentList: ListBuffer[Document] = ListBuffer()

    val list: ListBuffer[(String, String, Double)] = ListBuffer()

    var page = 1
    breakable {
      while(true) {
        val pageSource = getSourceCode(product, page)
        if(!hasContentToProcess(pageSource, page)) break
        else contentList.append(pageSource)
        println("AllegroActor: got result page " + page)
        page = page + 1
      }
    }

    contentList.foldLeft(list)((l, d) => l ++= processPage(d))
    println("AllegroActor: got products: " + list.size + " from processed pages: " + contentList.size)

    list.reverse.sorted.toList
  }

  def hasContentToProcess(preparedDoc: Document, pageNumber: Integer): Boolean = {
    val hasContent = preparedDoc.body().getElementsByClass(iterableClass).size()>0
    if(hasContent) {
      return true
    } else {
      println(s"AllegroActor: search on page $pageNumber seems to return no results")
      return false
    }
  }

  def processPage(doc: Document): ListBuffer[(String, String, Double)] = {
    val iteration = doc.body().getElementsByClass(iterableClass)
    val productList: java.util.Iterator[Element] = iteration.iterator()
    val pageList = scala.collection.mutable.ListBuffer.empty[(String, String, Double)]
    while(productList.hasNext()) {
      val currentProduct = productList.next()
      val currentProductPrice = pricePattern.findFirstIn(currentProduct.getElementsByClass(priceClass).text().replaceAll(" ", "")).get
      val currentProductName = currentProduct.getElementsByClass("details").first().getElementsByTag("h2").text()
      val currentProductLink = "http://allegro.pl" + currentProduct.getElementsByClass("details").first().getElementsByTag("a").first().attr("href")

      pageList += ((currentProductLink, currentProductName, currentProductPrice.replace(",",".").toDouble))
    }
    pageList
  }

  override def getDescription(id: String): String = {
    val pageContent = Jsoup.connect(id).get().getElementById(descriptionId)
    if(pageContent != null) return pageContent.text()
    else return "Zobacz na " + id
  }
}

class AllegroActorRef(override val actorRef: ActorRef, override val name: String)
    extends CrawlerActorRef(actorRef, name) {
}

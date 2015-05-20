package actor.mining

import akka.actor.{ActorRef}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._
import scala.util.matching.Regex
object OlxActor {

  val pricePattern = new Regex("[0-9]+")
  val urlTrashPattern = "#[^&]+"
  val priceClass = "price"
  val iterableClass = "offer"
  val normalOfferTable = "offers_table"

  def getSourceCode(product: String, page: Integer): Document = {
    // Olx parametry GETa
    // page=1 - numer strony z wynikami, jesli damy za duzy to przekieruje nas do http://olx.pl/oferty/?page=xx czyli wywali
    // nasze zapytanie
    // search[description]=1 - tak jak w allegro - szuka także w parametrach i opisach
    // search[order]=filter_float_price%3Aasc - sortuje od najtanszych
    // search[order]=filter_float_price%3Adesc - sortuje od najdrozszych
    // search[order]=created_at%3Adesc - sortuje od najnowszych
    // view=list -> widok listy; galleryWide - galeria 1; galleryBig - galeria typ 2
    // TODO: warunek stopu dla pętli pobierania - sprawdzenie czy to co dostajemy ma w adresie q-$product
    println("getting product " + product + ", page " + page)
    Jsoup.connect(s"http://olx.pl/oferty/q-$product/?page=$page").get()
  }
}


class OlxActor extends CrawlerActor {
  import OlxActor._

  override def getPrices(product: String): List[(String, String, Double)] = {
    val contentList: ListBuffer[Document] = ListBuffer()

    val list: ListBuffer[(String, String, Double)] = ListBuffer()

    if(product.length < 0) return list.toList

    var page = 1
    breakable {
      while(true) {
        val pageSource = getSourceCode(product, page)
        println("got page " + page)
        if(!hasContentToProcess(pageSource, product)) break
        else contentList.append(pageSource)
        page = page + 1
      }
    }

    contentList.foldLeft(list)((l, d) => l ++= processPage(d))
    println("wczytalem produktow: " + list.size + " z rozpoznanych stron: " + contentList.size)

    list.reverse.sorted.toList
  }

  def hasContentToProcess(preparedDoc: Document, product: String): Boolean = {
    val docLocation = preparedDoc.location()
    println("Processing address " + docLocation)
    if(!(docLocation.contains(product.toLowerCase.replaceAll(" ", "-"))
      || docLocation.contains(product.toLowerCase.replaceAll(" ", "%20")))) {
      println("not processing, has q-" + product + " in address")
      return false
    }

    val hasContent = preparedDoc.body().getElementsByClass(iterableClass).size()>0
    if(hasContent) {
      println("processing, has content")
      return true
    } else {
      println("not processing, has no content")
      return false
    }
  }

  def processPage(doc: Document): ListBuffer[(String, String, Double)] = {
    // wzięcie ofert tylko z tabeli normalnych ofert, załatwia nam odfiltrowanie powtórzonych ofert wyróżnionych jednocześnie
    // nie wywalając ofert wyróżnionych
    val iteration = doc.body().getElementById(normalOfferTable).getElementsByClass(iterableClass)
    val productList: java.util.Iterator[Element] = iteration.iterator()
    val pageList = scala.collection.mutable.ListBuffer.empty[(String, String, Double)]
    while(productList.hasNext()) {
      val currentProduct = productList.next()
      if(currentProduct.getElementsByClass(priceClass).size()>0) {
        breakable {
          val currentProductPriceOcc = pricePattern.findFirstIn(currentProduct.getElementsByClass(priceClass).text().replaceAll(" ", ""))
          if(currentProductPriceOcc.size==0) break //konstrukcja continue w przypadku gdy nie mamy ceny a np. "Zamienię" lub "Oddam"
          val currentProductPrice = currentProductPriceOcc.get
          val currentProductName = currentProduct.getElementsByTag("h3").first().text()
          val currentProductLink = currentProduct.getElementsByTag("h3").first().getElementsByTag("a").first().attr("href").replaceAll(urlTrashPattern, "")
          pageList += ((currentProductLink, currentProductName, currentProductPrice.replace(",",".").toDouble))
        }
      }
    }
    pageList
  }

  override def getDescription(id: String): String = s"Olx $id: MOCK"
}

class OlxActorRef(override val actorRef: ActorRef, override val name: String)
    extends CrawlerActorRef(actorRef, name) {
}

package actor.mining

import akka.actor.ActorRef
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
  val descriptionId = "textContent"

  def getSourceCode(product: String, page: Integer): Document = {
    // Olx parametry GETa
    // page=1 - numer strony z wynikami, jesli damy za duzy to przekieruje nas do http://olx.pl/oferty/?page=xx czyli wywali
    // nasze zapytanie
    // search[description]=1 - tak jak w allegro - szuka także w parametrach i opisach
    // search[order]=filter_float_price%3Aasc - sortuje od najtanszych
    // search[order]=filter_float_price%3Adesc - sortuje od najdrozszych
    // search[order]=created_at%3Adesc - sortuje od najnowszych
    // view=list -> widok listy; galleryWide - galeria 1; galleryBig - galeria typ 2
    // warunek stopu dla pętli pobierania - sprawdzenie czy to co dostajemy ma w adresie q-$product
    //println("OlxActor: getSourceCode, search product " + product + ", page " + page)
    Jsoup.connect(s"http://olx.pl/oferty/q-$product/?page=$page").get()
  }
}


class OlxActor extends CrawlerActor {
  import OlxActor._

  override def getPrices(productAndCategory: String): List[(String, String, Double)] = {
    println("OlxActor: getPrices")

    val product = productAndCategory.split("\\?")(0)
    var category: String = ""
    if(productAndCategory.split("\\?").length > 1) {
      category = productAndCategory.split("\\?")(1)
    }

    val contentList: ListBuffer[Document] = ListBuffer()

    val list: ListBuffer[(String, String, Double)] = ListBuffer()

    if(product.length < 1) return list.toList

    var page = 1
    val endPage = 10 //getMaxPageResult(getSourceCode(product, 1))
    breakable {
      while(true) {
        val pageSource = getSourceCode(product, page)
        if(hasContentToProcess(pageSource, product, page)) contentList.append(pageSource)
        println("OlxActor: got result page " + page + " of " + endPage)
        if (page == endPage) break
        page = page + 1
      }
    }

    contentList.foldLeft(list)((l, d) => l ++= processPage(d, category))
    println("OlxActor: got products: " + list.size + " from processed pages: " + contentList.size)

    list.reverse.sorted.toList
  }

  def hasContentToProcess(preparedDoc: Document, product: String, pageNumber: Integer): Boolean = {
    if(preparedDoc.body().getElementsByClass("emptynew").size() > 0) {
      println(s"OlxActor: search on page $pageNumber seems to return no results")
      return false
    }

    val hasContent = preparedDoc.body().getElementsByClass(iterableClass).size()>0
    if(hasContent) {
      return true
    } else {
      return false
    }
  }

  def getMaxPageResult(preparedDoc: Document): Integer = {
    val potentialMaxPageNumber = preparedDoc.select(".block.br3.brc8.large.tdnone.lheight24")
    if(potentialMaxPageNumber == null || potentialMaxPageNumber.size()==0) {
      println("OlxActor: getMaxPageResult: 1")
      return 1
    }
    else return {
      println("OlxActor: getMaxPageResult: " + potentialMaxPageNumber.last().text())
      potentialMaxPageNumber.last().text().toInt
    }
  }

  def processPage(doc: Document, category: String): ListBuffer[(String, String, Double)] = {
    // wzięcie ofert tylko z tabeli normalnych ofert, załatwia nam odfiltrowanie powtórzonych ofert wyróżnionych jednocześnie
    // nie wywalając ofert wyróżnionych
    val pageList = scala.collection.mutable.ListBuffer.empty[(String, String, Double)]

    val iteration = doc.body().getElementById(normalOfferTable).getElementsByClass(iterableClass)
    val productList: java.util.Iterator[Element] = iteration.iterator()
    var number = 0;
    while(productList.hasNext()) {
      val currentProduct = productList.next()
      if(currentProduct.getElementsByClass(priceClass).size()>0) {
        breakable {
          val currentProductPriceOcc = pricePattern.findFirstIn(currentProduct.getElementsByClass(priceClass).text().replaceAll(" ", ""))
          if(currentProductPriceOcc.size==0) break //konstrukcja continue w przypadku gdy nie mamy ceny a np. "Zamienię" lub "Oddam"
          val currentProductPrice = currentProductPriceOcc.get
          val currentProductName = currentProduct.getElementsByTag("h3").first().text()
          val currentProductLink = currentProduct.getElementsByTag("h3").first().getElementsByTag("a").first().attr("href").replaceAll(urlTrashPattern, "")

          if(preProcessProduct(currentProductLink, category)) {
            pageList += ((currentProductLink, currentProductName, currentProductPrice.replace(",",".").toDouble))
          }
        }
      }
    }
    pageList
  }

  def preProcessProduct(link: String, desiredCategory: String): Boolean = {
    print("OlxActor: preProcessingProduct " + link)
    if (desiredCategory==null || desiredCategory.length()==0) {
      println(" ok")
      return true
    }
    val doc = Jsoup.connect(link).get()
    val categories = doc.select("#breadcrumbTop .inline")
    val catIterator = categories.iterator()
    while(catIterator.hasNext) {
      val selectedCat = catIterator.next()
      if(selectedCat.text.toLowerCase.contains(desiredCategory)) {
        println(" ok")
        return true
      }
    }
    println(" failed")
    return false
  }

  override def getDescription(id: String): String = {
    val pageContent = Jsoup.connect(id).get().getElementById(descriptionId)
    if(pageContent != null) return pageContent.text()
    else return "Zobacz na " + id
  }
}

class OlxActorRef(override val actorRef: ActorRef, override val name: String)
    extends CrawlerActorRef(actorRef, name) {
}

package actor.mining

import akka.actor.ActorRef
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._
import scala.util.matching.Regex

object GumtreeActor {

  val pricePattern = new Regex("[0-9]+")
  val priceClass = "ar-price"
  val productTableId = "SNB_Results"
  val iterableClass = "resultsTableSB"
  val titleClass = "ar-title"
  val linkClass = "adLinkSB"
  val descriptionId = "ad-desc"

  def getSourceCode(product: String, page: Integer): Document = {
    // Gumtree parametry GET'a
    // Sort=1 sortowanie -> 1 - sortuje po dacie od najstarszych; 2 - po dacie od najnowszych; 3 - po cenie od najnizszej; 4 - po cenie od najwyzszej
    // wieksze od 4 - po dacie od najnowszych
    // AdType=2 wybor rodzaju ogloszenie -> 1 - ogłoszenie "poszukuję"; 2 - "oferuję"; inne to ogłoszenia pracy itp
    // gallery=false -> false - widok listy; true - widok galerii
    // Page=1 numer strony z wynikami -> podanie wiekszego numeru strony niz ostatni (np. 4 gdzie mamy 1-3) powoduje wyswietlenie ostatniej strony wynikow (czyli np. 3)
    //TODO: parsowanie tytułów bo gumtree sprawdza zarówno w opisie i tytule
    //warunek stopu pętli pobierania - sprawdzenie czy kolejna strona zwraca taką samą treść jak poprzednia
    //println("GumtreeActor: getSourceCode, search product " + product + ", page " + page)
    Jsoup.connect(s"http://www.gumtree.pl/fp-$product?Page=$page").get()
  }
}


class GumtreeActor extends CrawlerActor {
  import GumtreeActor._

  override def getPrices(productAndCategory: String): List[(String, String, Double)] = {
    println("GumtreeActor: getPrices")

    val product = productAndCategory.split("\\?")(0)
    var category = ""
    if(productAndCategory.split("\\?").length > 1) {
      category = productAndCategory.split("\\?")(1)
    }

    val contentList: ListBuffer[Document] = ListBuffer()

    val list: ListBuffer[(String, String, Double)] = ListBuffer()

    if (product.length < 1) return list.toList

    var page = 1
    val endPage = getMaxPageResult(getSourceCode(product, 1))
    breakable {
      while (true) {
        val pageSource = getSourceCode(product, page)
        if(hasContentToProcess(pageSource, product, page)) {
          println("GumtreeActor: got result page " + page + " of " + endPage)
          contentList.append(pageSource)
        }
        if (page == endPage) break
        page = page + 1
      }
    }

    contentList.foldLeft(list)((l, d) => l ++= processPage(d, category, product))
    println("GumtreeActor: got products: " + list.size + " from processed pages: " + contentList.size)

    list.reverse.sorted.toList
  }

  def getMaxPageResult(preparedDoc: Document): Integer = {
    val potentialMaxPageNumber = preparedDoc.getElementsByClass("notCurrentPage")
    if(potentialMaxPageNumber.size()==0) return 1
    else return potentialMaxPageNumber.last().text().toInt
  }

  def hasContentToProcess(preparedDoc: Document, product: String, pageNumber: Integer): Boolean = {
    val productTable = preparedDoc.body().getElementById(productTableId)
    if(productTable == null) {
      println(s"GumtreeActor: search on page $pageNumber seems to return no results")
      return false
    } else {
      return true
    }
  }

  def processPage(doc: Document, category: String, product: String): ListBuffer[(String, String, Double)] = {
    // wzięcie ofert tylko z tabeli normalnych ofert, załatwia nam odfiltrowanie powtórzonych ofert wyróżnionych jednocześnie
    // nie wywalając ofert wyróżnionych
    val pageList = scala.collection.mutable.ListBuffer.empty[(String, String, Double)]
    val productTable = doc.body().getElementById(productTableId)
    if(productTable == null) return pageList

    val iteration = productTable.getElementsByClass(iterableClass)
    val productList: java.util.Iterator[Element] = iteration.iterator()

    while(productList.hasNext()) {
      val currentProduct = productList.next()
      if(currentProduct.getElementsByClass(priceClass).size()>0) {
        breakable {
          val currentProductPriceOcc = pricePattern.findFirstIn(currentProduct.getElementsByClass(priceClass).text().replaceAll(" ", ""))
          if(currentProductPriceOcc.size==0) break //konstrukcja continue w przypadku gdy nie mamy ceny a np. "Proszę o kontakt" lub "Oddam"
          val currentProductPrice = currentProductPriceOcc.get
          val currentProductName = currentProduct.getElementsByClass(titleClass).first().text()
          val currentProductLink = currentProduct.getElementsByClass(linkClass).first().attr("href")

          if(preProcessProduct(currentProductLink, category, currentProductName, product)) {
            pageList += ((currentProductLink, currentProductName, currentProductPrice.replace(",",".").toDouble))
          }
        }
      }
    }
    pageList
  }

  def preProcessProduct(link: String, desiredCategory: String, title: String, product: String): Boolean = {
    print("GumtreeActor: preProcessingProduct " + link)
    val queryElements = product.split(" ")
    var contains = true
    breakable {
      for(i <- 0 to queryElements.length-1) {
        print(" checking " + queryElements(i))
        if(queryElements(i).length==1) {
          if(!title.toLowerCase.contains(queryElements(i) + " ") ||
             !title.toLowerCase.contains(" " + queryElements(i))) {
            contains = false
            break
          }
        } else if(!title.toLowerCase.contains(queryElements(i))) {
          contains = false
          break
        }
      }
    }

    if (!contains) {
      println(" failed - none of query elements in title")
      return false
    }
    if (title.toLowerCase.contains("poszuk")
      || title.toLowerCase.contains("szukam")
      || title.toLowerCase.contains("zamien")
      || title.toLowerCase.contains("zamian")) {
      println(" failed - search")
      return false
    }
    if (desiredCategory==null || desiredCategory.length()==0 || link.contains("cp-" + desiredCategory)) {
      println(" ok")
      return true
    }
    val doc = Jsoup.connect(link).get()
    val categories = doc.select("#breadcrumbVIP a")
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

class GumtreeActorRef(override val actorRef: ActorRef, override val name: String)
    extends CrawlerActorRef(actorRef, name) {
}

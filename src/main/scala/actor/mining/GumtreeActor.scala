package actor.mining

import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._
import scala.util.matching.Regex

object GumtreeActor {

  val pricePattern = new Regex("[0-9]+")
  val priceClass = "ar-price"
  val productTableId = "SNB_Results"
  val iterableClass = "resultsTableSB"
  var lastProcessedContent = new Elements()

  def getSourceCode(product: String, page: Integer): Document = {
    // Gumtree parametry GET'a
    // Sort=1 sortowanie -> 1 - sortuje po dacie od najstarszych; 2 - po dacie od najnowszych; 3 - po cenie od najnizszej; 4 - po cenie od najwyzszej
    // wieksze od 4 - po dacie od najnowszych
    // AdType=2 wybor rodzaju ogloszenie -> 1 - ogłoszenie "poszukuję"; 2 - "oferuję"; inne to ogłoszenia pracy itp
    // gallery=false -> false - widok listy; true - widok galerii
    // Page=1 numer strony z wynikami -> podanie wiekszego numeru strony niz ostatni (np. 4 gdzie mamy 1-3) powoduje wyswietlenie ostatniej strony wynikow (czyli np. 3)
    //TODO: parsowanie tytułów bo gumtree sprawdza zarówno w opisie i tytule
    //TODO: warunek stopu pętli pobierania - sprawdzenie czy kolejna strona zwraca taką samą treść jak poprzednia
    println("GumtreeActor: getSourceCode, search product " + product + ", page " + page)
    Jsoup.connect(s"http://www.gumtree.pl/fp-$product?Page=$page").get()
  }
}


class GumtreeActor extends CrawlerActor {
  import GumtreeActor._

  override def getPrices(product: String): List[(String, String, Double)] = {
    println("GumtreeActor: getPrices")

    val contentList: ListBuffer[Document] = ListBuffer()

    val list: ListBuffer[(String, String, Double)] = ListBuffer()

    if(product.length < 0) return list.toList

    var page = 1
    val endPage = getMaxPageResult(getSourceCode(product, 1))
    println("endpage is " + endPage)
    breakable {
      while(true) {
        val pageSource = getSourceCode(product, page)
        println("got page " + page)
        contentList.append(pageSource)
        if(page == endPage) break
        page = page + 1
      }
    }

    contentList.foldLeft(list)((l, d) => l ++= processPage(d))
    println("wczytalem produktow: " + list.size + " z rozpoznanych stron: " + contentList.size)

    list.reverse.sorted.toList

//    var list: List[(String, String, Double)] = List()
//
//    //malo funkcyjnie, wykorzystana javowa biblioteka Jsoup
//    val doc = getSourceCode(product)
//    val prices = doc.body().getElementsByClass(priceClass)
//    val price: java.util.Iterator[Element] = prices.iterator()
//    while(price.hasNext()) {
//      val cur_price = price.next()
//      if(cur_price.children().size() > 0) {
//        val ext_price = pricePattern.findFirstIn(cur_price.child(0).text().replaceAll(" ", ""))
//        if(!ext_price.isEmpty) {
//          list = ("id", "nazwa", ext_price.get.replace(",",".").toDouble) :: list
//        }
//      }
//    }
//    list.reverse
  }

  def getMaxPageResult(preparedDoc: Document): Integer = {
    val potentialMaxPageNumber = preparedDoc.getElementsByClass("notCurrentPage")
    if(potentialMaxPageNumber.size()==0) return 1
    else return potentialMaxPageNumber.last().text().toInt
  }

//  def hasContentToProcess(preparedDoc: Document, product: String): Boolean = {
//    val hasContent = preparedDoc.body().getElementsByClass(iterableClass).size()>0
//    if(hasContent) {
//      println("starting hasContentToProcess")
//      val currentElements = preparedDoc.body().getElementsByClass(iterableClass)
//
//      if(lastProcessedContent == null || lastProcessedContent.size()==0) {
//        println("has content and is first page")
//        lastProcessedContent = currentElements
//        return true
//      } else {
//        val actualSet = currentElements.toArray.toSet
//        val lastSet = lastProcessedContent.toArray.toSet
//        println("----ACTUAL SET-----")
//        println(actualSet)
//        println("----LAST SET-----")
//        println(lastSet)
//
//        if(actualSet.equals(lastSet)) {
//          lastProcessedContent = currentElements
//          println("this page has same elements as previous, not processing")
//          return false
//        } else {
//          lastProcessedContent = currentElements
//          println("has content, processing")
//          return true
//        }
//      }
//    } else {
//      println("not processing, has no content")
//      return false
//    }
//  }

  def processPage(doc: Document): ListBuffer[(String, String, Double)] = {
    // wzięcie ofert tylko z tabeli normalnych ofert, załatwia nam odfiltrowanie powtórzonych ofert wyróżnionych jednocześnie
    // nie wywalając ofert wyróżnionych
    val iteration = doc.body().getElementById(productTableId).getElementsByClass(iterableClass)
    val productList: java.util.Iterator[Element] = iteration.iterator()
    val pageList = scala.collection.mutable.ListBuffer.empty[(String, String, Double)]
    while(productList.hasNext()) {
      val currentProduct = productList.next()
      if(currentProduct.getElementsByClass(priceClass).size()>0) {
        breakable {
          val currentProductPriceOcc = pricePattern.findFirstIn(currentProduct.getElementsByClass(priceClass).text().replaceAll(" ", ""))
          if(currentProductPriceOcc.size==0) break //konstrukcja continue w przypadku gdy nie mamy ceny a np. "Proszę o kontakt" lub "Oddam"
          val currentProductPrice = currentProductPriceOcc.get
          println(currentProductPrice)
          val currentProductName = currentProduct.getElementsByClass("ar-title").first().text()
          println(currentProductName)
          val currentProductLink = currentProduct.getElementsByClass("adLinkSB").first().attr("href")
          println(currentProductLink)

          pageList += ((currentProductLink, currentProductName, currentProductPrice.replace(",",".").toDouble))
        }
      }
    }
    pageList
  }

  override def getDescription(id: String): String = s"Gumtree $id: MOCK"
}

class GumtreeActorRef(override val actorRef: ActorRef, override val name: String)
    extends CrawlerActorRef(actorRef, name) {
}

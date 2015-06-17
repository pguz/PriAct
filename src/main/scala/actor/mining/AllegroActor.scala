package actor.mining

import akka.actor.ActorRef
import cc.mallet.classify.{Classification, Classifier}
import cc.mallet.pipe.Pipe
import cc.mallet.types.Instance
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import scala.util.matching.Regex

object AllegroActor  {
  val pricePattern = new Regex("""\d+,\d{2}?""")
  val priceClass = "price"
  val iterableClass = "offer"
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
    Jsoup.connect(s"http://allegro.pl/listing/listing.php?bmatch=seng-ps-mp-p-sm-isqm-2-e-0402&description=0&limit=180&string=$product&p=$page&offerTypeBuyNow=1").get()
  }
}


class AllegroActor extends CrawlerActor {
  import AllegroActor._
  import scala.util.control.Breaks._
  import scala.collection.mutable.ListBuffer
  import java.nio.file.{Paths, Files}
  import java.nio.charset.StandardCharsets

  override def getPrices(productAndCategory: String): List[(String, String, Double)]= {
    println("AllegroActor: getPrices")
    val classifier = new TextClassifier(productAndCategory, "allegro")
    val contentList: ListBuffer[Document] = ListBuffer()
    val product = productAndCategory.split("\\?")(0)
    var category = ""
    if(productAndCategory.split("\\?").length > 1) {
      category = productAndCategory.split("\\?")(1)
    }
    println("AllegroActor: after splits")
    val list: ListBuffer[(String, String, Double)] = ListBuffer()
    println("AllegroActor: after list init")

    if(product.length < 1) return list.toList

    var page = 1
    var endPage = getMaxPageResult(getSourceCode(product, 1))
    val maxEndPage = 999

    // limitowanie wyników
    if(endPage > maxEndPage) {
      endPage = maxEndPage
      println("AllegroActor: limiting pages to " + endPage)
    }
    breakable {
      while(true) {
        val pageSource = getSourceCode(product, page)
        if(hasContentToProcess(pageSource, page)) contentList.append(pageSource)
        println("AllegroActor: got result page " + page + " of " + endPage)
        if (page == endPage) break
        page = page + 1
      }
    }

    contentList.foldLeft(list)((l, d) => l ++= processPage(d, category, classifier, product))
    println("AllegroActor: got products: " + list.size + " from processed pages: " + contentList.size)

    list.reverse.sorted.toList
  }

  def hasContentToProcess(preparedDoc: Document, pageNumber: Integer): Boolean = {
    val hasContent = preparedDoc.body().getElementsByClass(iterableClass).size()>0
    if(hasContent) {
      return true
    } else {
      println(s"AllegroActor: search on page $pageNumber seems to return no results")
      println(s"AllegroActor: check URL " + preparedDoc.location())
      println("--DUMP START--")
      Files.write(Paths.get("alldump.html"), preparedDoc.outerHtml().getBytes(StandardCharsets.UTF_8))
      println("--DUMP END--")
      return false
    }
  }

  def isAllDigits(x: String) = x forall Character.isDigit

  def getMaxPageResult(preparedDoc: Document): Integer = {
    println("AllegroActor: getMaxPageResult start")
    val potentialMaxPageNumberContainer = preparedDoc.select(".pager-nav").select("li")
    val pageNumIterator = potentialMaxPageNumberContainer.iterator()
    var pageNum = 1
    while(pageNumIterator.hasNext) {
      val selpageNum = pageNumIterator.next().text()
      if(selpageNum != null &&
         selpageNum.length()>0 &&
         isAllDigits(selpageNum) &&
         selpageNum.toInt > pageNum) {
        pageNum = selpageNum.toInt
      }
    }
    println("AllegroActor: getMaxPageResult end " + pageNum)
    return pageNum
  }

  def processPage(doc: Document, category: String, classifier: TextClassifier, productQuery: String): ListBuffer[(String, String, Double)] = {
    val iteration = doc.body().getElementsByClass(iterableClass)
    val productList: java.util.Iterator[Element] = iteration.iterator()
    val pageList = scala.collection.mutable.ListBuffer.empty[(String, String, Double)]
    while(productList.hasNext()) {
      breakable {
        val currentProduct = productList.next()
        val currentProductPriceFound = pricePattern.findFirstIn(currentProduct.getElementsByClass(priceClass).text().replaceAll(" ", ""))
        if(currentProductPriceFound.size<1) break //continue
        val currentProductPrice = currentProductPriceFound.get
        val currentProductName = currentProduct.getElementsByClass("details").first().getElementsByTag("h2").text()
        val currentProductLink = "http://allegro.pl" + currentProduct.getElementsByClass("details").first().getElementsByTag("a").first().attr("href")
        var currentProductDescriptionAndCategory:(Element, Elements) = null
        var productAsInstance:(Instance) = null
        if(category.length > 0) {
          currentProductDescriptionAndCategory = getDescriptionAndCategories(currentProductLink)
          productAsInstance = classifier.prepareInstance(currentProductName, currentProductLink, currentProductPrice.toString,
            currentProductDescriptionAndCategory._2.text(), currentProductDescriptionAndCategory._2.text(), productQuery, category)
        }

        if(classifier.getClassifier!=null && classifyProduct(currentProductLink, productAsInstance, classifier)) {
          pageList += ((currentProductLink, currentProductName, currentProductPrice.replace(",",".").toDouble))
        } else if (classifier.getClassifier == null && preProcessProduct(currentProductLink, category, currentProductDescriptionAndCategory)) {
          pageList += ((currentProductLink, currentProductName, currentProductPrice.replace(",",".").toDouble))
        }
      }
    }
    pageList
  }

  def getDescriptionAndCategories(link: String): (Element, Elements) = {
    val doc = Jsoup.connect(link).get()
    var categories = doc.select(".itemscope span[itemprop=\"title\"]")
    if(categories.size()==0) categories = doc.select(".breadcrumb-container")
    val description = doc.getElementById(descriptionId);
    (description, categories)
  }

  def classifyProduct(link: String, product: Instance, classifier: TextClassifier): Boolean = {
    classifier.classify(product, link)
  }

  def preProcessProduct(link: String, desiredCategory: String, descriptionAndCategories: (Element, Elements)): Boolean = {
    print("AllegroActor: preProcessingProduct " + link + " without classifier")
    if (desiredCategory==null || desiredCategory.length()==0) {
      println(" and without category ok")
      return true
    }
    val categories = descriptionAndCategories._2
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

class AllegroActorRef(override val actorRef: ActorRef, override val name: String)
    extends CrawlerActorRef(actorRef, name) {
}

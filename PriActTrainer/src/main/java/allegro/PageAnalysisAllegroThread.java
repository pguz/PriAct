package allegro;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * Created by sopello on 01.06.2015.
 */
public class PageAnalysisAllegroThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(PageAnalysisAllegroThread.class);
    private Set<Document> contentSet;
    private String product;
    private Integer page;
    private Integer endPage;

    public PageAnalysisAllegroThread(Set<Document> contentSet, String product, Integer page, Integer endPage) {
        super();
        this.contentSet = contentSet;
        this.product = product;
        this.page = page;
        this.endPage = endPage;
    }

    private void performPageProcessing() {
        Document pageSource = getSourceCode(product, page);
        if (hasContentToProcess(pageSource, page)) contentSet.add(pageSource);
        log.info("AllegroActor: got result page " + page + " of " + endPage);
    }

    private Document getSourceCode(String product, Integer page) {
        // Allegro parametry GET'a
        // description=1 -> wyszukuje także w opisach i parametrach
        // p=1 -> numer strony, jesli wyjdziemy wyzej to wyjdzie nam komunikat z pusta strona ("Wygląda na to, że nie mamy tego, czego szukasz.")
        // limit=180 -> limit ilosci wpisow na strone - dostepne 60, 120, 180, inna wartosc wyswietli 60
        // string=asd wyszukiwany tekst
        // offerTypeBuyNow=1 -> tylko kup teraz
        //warunek stopu dla pętli pobierania: sprawdzenie czy na stronie wynikowej mamy komunikat zawarty wyżej
        //println("AllegroActor: getSourceCode, search product " + product + ", page " + page)
        String address = "http://allegro.pl/listing/listing.php?bmatch=seng-ps-mp-p-sm-isqm-2-e-0402&order=p&description=0&limit=180&string=$product&p=$page&offerTypeBuyNow=1".replace("$product", product).replace("$page", page.toString());
        try {
            return Jsoup.connect(address).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean hasContentToProcess(Document preparedDoc, Integer pageNumber) {
        boolean hasContent = preparedDoc.body().getElementsByClass(AllegroAnalyzer.iterableClass).size() > 0;
        if (hasContent) {
            return true;
        } else {
            log.error("AllegroActor: search on page $pageNumber seems to return no results".replace("$pageNumber", pageNumber.toString()));
            log.error("AllegroActor: check URL " + preparedDoc.location());
            return false;
        }
    }

    private String getDescription(String link) {
        Element pageContent = null;
        try {
            pageContent = Jsoup.connect(link).get().getElementById(AllegroAnalyzer.descriptionId);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        if(pageContent != null) return pageContent.text();
        else return "";
    }

    public void run() {
        performPageProcessing();
    }
}

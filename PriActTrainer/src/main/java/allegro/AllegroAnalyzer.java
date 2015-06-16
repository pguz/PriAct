package allegro;

import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import common.Product;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by sopello on 01.06.2015.
 */
public class AllegroAnalyzer {

    public static final Pattern pricePattern = Pattern.compile("\\d+,\\d{2}?");
    public static final String priceClass = "price";
    public static final String iterableClass = "offer";
    public static final String descriptionId = "user_field";
    public static long threadFiringInterval = 100;

    private final static Logger log = LoggerFactory.getLogger(AllegroAnalyzer.class);

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

    public Set<Pair<Product, Boolean>> getProducts(String productAndCategory) {
        Set<Document> contentSet = Sets.newConcurrentHashSet();
        Set<Pair<Product, Boolean>> productsWithMatchSet = Sets.newConcurrentHashSet();

        log.info("AllegroActor: inside getProducts");
        String product = productAndCategory.split("\\?")[0];
        String category = "";
        if (productAndCategory.split("\\?").length > 1) {
            category = productAndCategory.split("\\?")[1];
        }
        log.info("AllegroActor: after splits");

        if (product.length() < 1) return productsWithMatchSet;

        Integer page = 1;
        Integer endPage = getMaxPageResult(getSourceCode(product, 1));

        Set<Thread> pageAnalysisThreads = Sets.newHashSet();
        while (true) {
            pageAnalysisThreads.add(new PageAnalysisAllegroThread(contentSet, product, page, endPage));
            if (page.equals(endPage)) break;
            page = page + 1;
        }

        for (Thread t : pageAnalysisThreads) {
            t.start();
            try {
                Thread.sleep(AllegroAnalyzer.threadFiringInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (Thread t : pageAnalysisThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Set<Thread> pageThreads = Sets.newHashSet();
        for (Document d : contentSet) {
            pageThreads.add(new PageAllegroThread(d, category, productsWithMatchSet, product));
        }

        for (Thread t : pageThreads) {
            t.start();
            try {
                Thread.sleep(15*AllegroAnalyzer.threadFiringInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (Thread t : pageThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("AllegroActor: got products: " + productsWithMatchSet.size() + " from processed pages: " + contentSet.size());

        return productsWithMatchSet;
    }

    private Integer getMaxPageResult(Document preparedDoc) {
        log.info("AllegroActor: getMaxPageResult start");
        Elements potentialMaxPageNumberContainer = preparedDoc.select(".pager-nav").select("li");
        Integer pageNum = 1;
        for (Element selpage : potentialMaxPageNumberContainer) {
            String selpageNum = selpage.text();
            if (selpageNum != null && selpageNum.length() > 0 &&
                    selpageNum.matches("[0-9]+") &&
                    Ints.tryParse(selpageNum) > pageNum) {
                pageNum = Ints.tryParse(selpageNum);
            }
        }
        log.info("AllegroActor: getMaxPageResult end " + pageNum);
        return pageNum;
    }

}

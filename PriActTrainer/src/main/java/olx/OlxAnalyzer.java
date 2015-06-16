package olx;

import com.google.common.collect.Sets;
import common.Product;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by sopello on 01.06.2015.
 */
public class OlxAnalyzer {

    public static final Pattern pricePattern = Pattern.compile("[0-9]+");
    public static final String urlTrashPattern = "#[^&]+";
    public static final String priceClass = "price";
    public static final String iterableClass = "offer";
    public static final String descriptionId = "textContent";
    public static final String normalOfferTable = "offers_table";
    public static long threadFiringInterval = 100;

    private final static Logger log = LoggerFactory.getLogger(OlxAnalyzer.class);

    private Document getSourceCode(String product, Integer page) {
        // Olx parametry GETa
        // page=1 - numer strony z wynikami, jesli damy za duzy to przekieruje nas do http://olx.pl/oferty/?page=xx czyli wywali
        // nasze zapytanie
        // search[description]=1 - tak jak w allegro - szuka także w parametrach i opisach
        // search[order]=filter_float_price%3Aasc - sortuje od najtanszych
        // search[order]=filter_float_price%3Adesc - sortuje od najdrozszych
        // search[order]=created_at%3Adesc - sortuje od najnowszych
        // view=list -> widok listy; galleryWide - galeria 1; galleryBig - galeria typ 2
        // warunek stopu dla pętli pobierania - sprawdzenie czy to co dostajemy ma w adresie q-$product
        String address = "http://olx.pl/oferty/q-$product/?page=$page".replace("$product", product).replace("$page", page.toString());
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

        log.info("OlxActor: inside getProducts");
        String product = productAndCategory.split("\\?")[0];
        String category = "";
        if (productAndCategory.split("\\?").length > 1) {
            category = productAndCategory.split("\\?")[1];
        }
        log.info("OlxActor: after splits");

        if (product.length() < 1) return productsWithMatchSet;

        Integer page = 1;
        Integer endPage = getMaxPageResult(getSourceCode(product, 1));

        Set<Thread> pageAnalysisThreads = Sets.newHashSet();
        while (true) {
            pageAnalysisThreads.add(new PageAnalysisOlxThread(contentSet, product, page, endPage));
            if (page.equals(endPage)) break;
            page = page + 1;
        }

        for (Thread t : pageAnalysisThreads) {
            t.start();
            try {
                Thread.sleep(OlxAnalyzer.threadFiringInterval);
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
            pageThreads.add(new PageOlxThread(d, category, productsWithMatchSet, product));
        }

        for (Thread t : pageThreads) {
            t.start();
            try {
                Thread.sleep(15* OlxAnalyzer.threadFiringInterval);
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
        log.info("OlxActor: got products: " + productsWithMatchSet.size() + " from processed pages: " + contentSet.size());

        return productsWithMatchSet;
    }

    private Integer getMaxPageResult(Document preparedDoc) {
        log.info("OlxActor: getMaxPageResult start");
        Elements potentialMaxPageNumberContainer = preparedDoc.select(".block.br3.brc8.large.tdnone.lheight24");
        Integer pageNum = 1;
        if(potentialMaxPageNumberContainer == null || potentialMaxPageNumberContainer.size()==0) {
            pageNum = 1;
        } else {
            pageNum = Integer.valueOf(potentialMaxPageNumberContainer.last().text());
        }

        log.info("OlxActor: getMaxPageResult end " + pageNum);
        return pageNum;
    }

}

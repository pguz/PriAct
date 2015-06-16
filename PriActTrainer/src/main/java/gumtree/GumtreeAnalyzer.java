package gumtree;

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
public class GumtreeAnalyzer {

    public static final Pattern pricePattern = Pattern.compile("[0-9]+");
    public static final String priceClass = "ar-price";
    public static final String iterableClass = "resultsTableSB";
    public static final String descriptionId = "ad-desc";
    public static final String linkClass = "adLinkSB";
    public static final String productTableId = "SNB_Results";
    public static final String titleClass = "ar-title";

    public static long threadFiringInterval = 100;

    private final static Logger log = LoggerFactory.getLogger(GumtreeAnalyzer.class);

    private Document getSourceCode(String product, Integer page) {
        // Gumtree parametry GET'a
        // Sort=1 sortowanie -> 1 - sortuje po dacie od najstarszych; 2 - po dacie od najnowszych; 3 - po cenie od najnizszej; 4 - po cenie od najwyzszej
        // wieksze od 4 - po dacie od najnowszych
        // AdType=2 wybor rodzaju ogloszenie -> 1 - ogłoszenie "poszukuję"; 2 - "oferuję"; inne to ogłoszenia pracy itp
        // gallery=false -> false - widok listy; true - widok galerii
        // Page=1 numer strony z wynikami -> podanie wiekszego numeru strony niz ostatni (np. 4 gdzie mamy 1-3) powoduje wyswietlenie ostatniej strony wynikow (czyli np. 3)
        String address = "http://www.gumtree.pl/fp-$product?Page=$page".replace("$product", product).replace("$page", page.toString());
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

        log.info("GumtreeActor: inside getProducts");
        String product = productAndCategory.split("\\?")[0];
        String category = "";
        if (productAndCategory.split("\\?").length > 1) {
            category = productAndCategory.split("\\?")[1];
        }
        log.info("GumtreeActor: after splits");

        if (product.length() < 1) return productsWithMatchSet;

        Integer page = 1;
        Integer endPage = getMaxPageResult(getSourceCode(product, 1));

        Set<Thread> pageAnalysisThreads = Sets.newHashSet();
        while (true) {
            pageAnalysisThreads.add(new PageAnalysisGumtreeThread(contentSet, product, page, endPage));
            if (page.equals(endPage)) break;
            page = page + 1;
        }

        for (Thread t : pageAnalysisThreads) {
            t.start();
            try {
                Thread.sleep(GumtreeAnalyzer.threadFiringInterval);
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
            pageThreads.add(new PageGumtreeThread(d, category, productsWithMatchSet, product));
        }

        for (Thread t : pageThreads) {
            t.start();
            try {
                Thread.sleep(15* GumtreeAnalyzer.threadFiringInterval);
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
        log.info("GumtreeActor: got products: " + productsWithMatchSet.size() + " from processed pages: " + contentSet.size());

        return productsWithMatchSet;
    }

    private Integer getMaxPageResult(Document preparedDoc) {
        log.info("GumtreeActor: getMaxPageResult start");
        Elements potentialMaxPageNumberContainer = preparedDoc.getElementsByClass("notCurrentPage");
        Integer pageNum = 1;
        if(potentialMaxPageNumberContainer.size()==0) {
            pageNum = 1;
        } else {
            pageNum = Integer.valueOf(potentialMaxPageNumberContainer.last().text());
        }
        log.info("GumtreeActor: getMaxPageResult end " + pageNum);
        return pageNum;
    }

}

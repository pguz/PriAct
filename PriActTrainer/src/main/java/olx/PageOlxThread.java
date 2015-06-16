package olx;

import com.google.common.collect.Sets;
import common.Product;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Created by sopello on 01.06.2015.
 */
public class PageOlxThread extends Thread {

    private Set<Pair<Product, Boolean>> productsFromPage;
    private static final Logger log = LoggerFactory.getLogger(PageOlxThread.class);
    private boolean result;
    private Document doc;
    private String category;
    private String productQuery;

    public PageOlxThread(Document doc, String category, Set<Pair<Product, Boolean>> productsFromPage, String productQuery) {
        super();
        this.doc = doc;
        this.category = category;
        this.productsFromPage = productsFromPage;
        this.productQuery = productQuery;
    }

    private void performPageProcessing() {
        Elements iteration = doc.body().getElementById(OlxAnalyzer.normalOfferTable).getElementsByClass(OlxAnalyzer.iterableClass);
        Set<Thread> processThreads = Sets.newHashSet();
        for (Element currentProduct : iteration) {
            ProcessOlxThread processOlxThread = new ProcessOlxThread(category, productsFromPage, currentProduct, productQuery);
            processThreads.add(processOlxThread);
        }

        for (Thread t : processThreads) {
            t.start();
            try {
                Thread.sleep(2 * OlxAnalyzer.threadFiringInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (Thread t : processThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public void run() {
        performPageProcessing();
    }
}

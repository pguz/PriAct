package gumtree;

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
public class PageAnalysisGumtreeThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(PageAnalysisGumtreeThread.class);
    private Set<Document> contentSet;
    private String product;
    private Integer page;
    private Integer endPage;

    public PageAnalysisGumtreeThread(Set<Document> contentSet, String product, Integer page, Integer endPage) {
        super();
        this.contentSet = contentSet;
        this.product = product;
        this.page = page;
        this.endPage = endPage;
    }

    private void performPageProcessing() {
        Document pageSource = getSourceCode(product, page);
        if (hasContentToProcess(pageSource, page)) contentSet.add(pageSource);
        log.info("GumtreeActor: got result page " + page + " of " + endPage);
    }

    private Document getSourceCode(String product, Integer page) {
        String address = "http://www.gumtree.pl/fp-$product?Page=$page".replace("$product", product).replace("$page", page.toString());
        try {
            return Jsoup.connect(address).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean hasContentToProcess(Document preparedDoc, Integer pageNumber) {
        Element productTable = preparedDoc.body().getElementById(GumtreeAnalyzer.productTableId);
        if (productTable == null) {
            log.error("GumtreeActor: search on page $pageNumber seems to return no results".replaceAll("$pageNumber", pageNumber.toString()));
            return false;
        } else {
            return true;
        }
    }

    public void run() {
        performPageProcessing();
    }
}

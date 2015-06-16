package olx;

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
public class PageAnalysisOlxThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(PageAnalysisOlxThread.class);
    private Set<Document> contentSet;
    private String product;
    private Integer page;
    private Integer endPage;

    public PageAnalysisOlxThread(Set<Document> contentSet, String product, Integer page, Integer endPage) {
        super();
        this.contentSet = contentSet;
        this.product = product;
        this.page = page;
        this.endPage = endPage;
    }

    private void performPageProcessing() {
        Document pageSource = getSourceCode(product, page);
        if (hasContentToProcess(pageSource, page)) contentSet.add(pageSource);
        log.info("OlxActor: got result page " + page + " of " + endPage);
    }

    private Document getSourceCode(String product, Integer page) {
        String address = "http://olx.pl/oferty/q-$product/?page=$page".replace("$product", product).replace("$page", page.toString());
        try {
            return Jsoup.connect(address).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean hasContentToProcess(Document preparedDoc, Integer pageNumber) {
        if(preparedDoc.body().getElementsByClass("emptynew").size() > 0) {
            log.error("OlxActor: search on page $pageNumber seems to return no results".replaceAll("$pageNumber", pageNumber.toString()));
            log.error("OlxActor: check URL " + preparedDoc.location());
            return false;
        }

        boolean hasContent = preparedDoc.body().getElementsByClass(OlxAnalyzer.iterableClass).size() > 0;
        if (hasContent) {
            return true;
        } else {
            log.error("OlxActor: search on page $pageNumber seems to return no results".replace("$pageNumber", pageNumber.toString()));
            log.error("OlxActor: check URL " + preparedDoc.location());
            return false;
        }
    }

    private String getDescription(String link) {
        Element pageContent = null;
        try {
            pageContent = Jsoup.connect(link).get().getElementById(OlxAnalyzer.descriptionId);
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

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Created by sopello on 01.06.2015.
 */
public class ProcessAllegroThread extends Thread {

    private String desiredCategory;
    private Element currentProduct;
    private String queryProduct;
    private Set<Pair<Product, Boolean>> productsFromPage;
    private static final Logger log = LoggerFactory.getLogger(ProcessAllegroThread.class);

    public ProcessAllegroThread(String desiredCategory, Set<Pair<Product, Boolean>> productsFromPage, Element currentProduct,
                                String queryProduct) {
        super();
        this.productsFromPage = productsFromPage;
        this.desiredCategory = desiredCategory;
        this.currentProduct = currentProduct;
        this.queryProduct = queryProduct;
    }

    private void performProcessing() {
        String potentialPrice = currentProduct.getElementsByClass(AllegroAnalyzer.priceClass).text().replaceAll(" ", "");
        //log.info("Potential price to " + potentialPrice);
        Matcher pricePatternMatcher = AllegroAnalyzer.pricePattern.matcher(potentialPrice);
        if (!pricePatternMatcher.find()) return;
        String currentProductPriceFound = pricePatternMatcher.group();
        if (currentProductPriceFound.length() < 1) return;
        String currentProductName = currentProduct.getElementsByClass("details").first().getElementsByTag("h2").text();
        String currentProductLink = "http://allegro.pl" + currentProduct.getElementsByClass("details").first().getElementsByTag("a").first().attr("href");
        Double currentProductPrice = Double.parseDouble(currentProductPriceFound.replace(",","."));
        Pair<String, String> descriptionAndCategory = getDescription(currentProductLink);
        String currentProductDescription = descriptionAndCategory.getLeft();
        String currentProductCategory = descriptionAndCategory.getRight();
        Product contextProduct = new Product(currentProductName, currentProductLink, currentProductDescription,
                currentProductPrice, currentProductCategory, queryProduct, desiredCategory);

        if (desiredCategory == null || desiredCategory.length()==0) {
            log.info("ProcessAllegroThread: preProcessingProduct " + contextProduct.getUrl() + " ok - no category specified");
            productsFromPage.add(Pair.of(contextProduct, true));
            return;
        }

        if (currentProductCategory.toLowerCase().contains(desiredCategory.toLowerCase())) {
            log.info("ProcessAllegroThread: preProcessingProduct " + contextProduct.getUrl() + " ok");
            productsFromPage.add(Pair.of(contextProduct, true));
            return;
        }

        log.info("ProcessAllegroThread: preProcessingProduct " + contextProduct.getUrl() + " failed");
        log.error("preProcessingProduct failed where categories are " + currentProductCategory);
        productsFromPage.add(Pair.of(contextProduct, false));
        return;
    }

    private Pair<String,String> getDescription(String link) {
        Element pageContent = null;
        String description = "";
        String categories = "";
        Document doc = null;
        try {
            doc = Jsoup.connect(link).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            pageContent = Jsoup.connect(link).get().getElementById(AllegroAnalyzer.descriptionId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Elements categoriesInPage = null;
        if (doc != null) {
            categoriesInPage = doc.select(".itemscope span[itemprop=\"title\"]");
        } else {
            log.error("doc is null!");
        }
        if (categoriesInPage.size()==0) {
            categoriesInPage = doc != null ? doc.select(".breadcrumb-container") : null;
        }

        categories = categoriesInPage.text();

        if (pageContent != null) description = pageContent.text();

        return Pair.of(description, categories);
    }

    public void run() {
        performProcessing();
    }
}

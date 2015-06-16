import org.apache.commons.lang3.StringUtils;

/**
 * Created by sopello on 01.06.2015.
 */
public class Product {
    private String name;
    private String url;
    private String description;
    private Double price;
    private String categories;
    private String desiredCategory;
    private String query;

    public Product(String name, String url, String description, Double price, String categories, String query,
                   String desiredCategory) {
        this.name = name;
        this.url = url;
        this.description = description;
        this.price = price;
        this.categories = categories;
        this.query = query;
        this.desiredCategory = desiredCategory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;

        if (name != null ? !name.equals(product.name) : product.name != null) return false;
        if (!url.equals(product.url)) return false;
        if (description != null ? !description.equals(product.description) : product.description != null) return false;
        return !(price != null ? !price.equals(product.price) : product.price != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + url.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (price != null ? price.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(";;;");
        sb.append(url);
        sb.append(";;;");
        sb.append(price.toString());
        sb.append(";;;");
        sb.append(categories);
        sb.append(";;;");
        boolean categoriesContainsDesired = categories.toLowerCase().contains(desiredCategory.toLowerCase());
        sb.append(categoriesContainsDesired);
        sb.append(";;;");
        Integer queryCountInDescription = StringUtils.countMatches(description.toLowerCase(), query.toLowerCase());
        sb.append(queryCountInDescription.toString());
        sb.append(";;;");
        Integer queryCountInTitle = StringUtils.countMatches(name.toLowerCase(), query.toLowerCase());
        sb.append(queryCountInTitle.toString());
        sb.append(";;;");

        return sb.toString();
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }
}

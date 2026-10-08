package io.github.antondorovs.qa.api;

import io.github.antondorovs.qa.models.Product;
import io.github.antondorovs.qa.models.ProductCategory;
import io.github.antondorovs.qa.models.ProductRequest;
import io.github.antondorovs.qa.models.ProductsResponse;
import io.github.antondorovs.qa.utils.JsonFiles;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.antondorovs.qa.api.ApiSpecifications.jsonResponse;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;

@Tag("api")
class ProductsApiTest {
    private final ProductsClient client = new ProductsClient();

    @Test
    void returnsProductById() {
        Product product = client.getProduct(1).then()
                .spec(jsonResponse(200))
                .header("Content-Type", startsWith("application/json"))
                .extract().as(Product.class);

        assertEquals(1, product.id());
        assertFalse(product.title().isBlank());
        assertFalse(product.category().isBlank());
        assertTrue(product.price().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void returnsOnlySelectedProductFields() {
        client.getProductFields(1, "title", "price").then()
                .spec(jsonResponse(200))
                .body("$", aMapWithSize(3))
                .body("id", equalTo(1))
                .body("title", not(blankOrNullString()))
                .body("price", notNullValue());
    }

    @Test
    void returnsOnlySelectedFieldsForProductList() {
        List<Map<String, Object>> products = client.listProductFields(2, 0, "title", "price").then()
                .spec(jsonResponse(200)).extract().jsonPath().getList("products");

        assertEquals(2, products.size());
        for (Map<String, Object> product : products) {
            assertEquals(Set.of("id", "title", "price"), product.keySet());
            assertFalse(((String) product.get("title")).isBlank());
            assertTrue(product.get("price") instanceof Number);
        }
    }

    @ParameterizedTest(name = "limit={0}, skip={1}")
    @CsvSource({"1, 0", "5, 0", "5, 5"})
    void paginatesProducts(int limit, int skip) {
        ProductsResponse response = client.listProducts(limit, skip).then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        assertEquals(limit, response.limit());
        assertEquals(skip, response.skip());
        assertEquals(limit, response.products().size());
        assertTrue(response.total() >= skip + limit);
        Product firstExpected = client.listProducts(skip + limit, 0).then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class).products().get(skip);
        assertEquals(firstExpected.id(), response.products().getFirst().id());
    }

    @Test
    void sortsProductsByPriceInAscendingOrder() {
        ProductsResponse response = client.listProductsSortedBy(20, "price", "asc").then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        assertEquals(20, response.products().size());
        for (int index = 1; index < response.products().size(); index++) {
            BigDecimal previousPrice = response.products().get(index - 1).price();
            BigDecimal currentPrice = response.products().get(index).price();
            assertTrue(previousPrice.compareTo(currentPrice) <= 0,
                    "Prices are not sorted at index " + index);
        }
    }

    @Test
    void sortsPaginatedProductsByPriceInDescendingOrder() {
        ProductsResponse response = client.listProductsSortedBy(5, 5, "price", "desc").then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        assertAll(
                () -> assertEquals(5, response.limit()),
                () -> assertEquals(5, response.skip()),
                () -> assertEquals(5, response.products().size())
        );
        for (int index = 1; index < response.products().size(); index++) {
            BigDecimal previousPrice = response.products().get(index - 1).price();
            BigDecimal currentPrice = response.products().get(index).price();
            assertTrue(previousPrice.compareTo(currentPrice) >= 0,
                    "Prices are not sorted at index " + index);
        }
    }

    @Test
    void preservesSortedPageIdentity() {
        ProductsResponse fullResult = client.listProductsSortedBy(10, "price", "desc").then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);
        ProductsResponse pagedResult = client.listProductsSortedBy(5, 5, "price", "desc").then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        List<Integer> expectedIds = fullResult.products().subList(5, 10).stream().map(Product::id).toList();
        List<Integer> actualIds = pagedResult.products().stream().map(Product::id).toList();
        assertEquals(expectedIds, actualIds);
    }

    @Test
    void sortsProductsByTitleInAscendingOrder() {
        ProductsResponse response = client.listProductsSortedBy(20, "title", "asc").then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        assertEquals(20, response.products().size());
        for (int index = 1; index < response.products().size(); index++) {
            String previousTitle = response.products().get(index - 1).title();
            String currentTitle = response.products().get(index).title();
            assertTrue(previousTitle.compareToIgnoreCase(currentTitle) <= 0,
                    "Titles are not sorted at index " + index);
        }
    }

    @Test
    void listsCategoriesUsedForProductFiltering() {
        client.listProductCategories().then()
                .spec(jsonResponse(200))
                .body("$", hasItems("beauty", "furniture", "smartphones"));
    }

    @Test
    void exposesCategoryDetailsForProductFiltering() {
        ProductCategory beauty = Arrays.stream(client.listProductCategoryDetails().then()
                        .spec(jsonResponse(200)).extract().as(ProductCategory[].class))
                .filter(category -> category.slug().equals("beauty"))
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertEquals("Beauty", beauty.name()),
                () -> assertEquals("https://dummyjson.com/products/category/beauty", beauty.url())
        );
    }

    @Test
    void keepsCategoryListAndCategoryDetailsInSync() {
        String[] categorySlugs = client.listProductCategories().then()
                .spec(jsonResponse(200)).extract().as(String[].class);
        ProductCategory[] categoryDetails = client.listProductCategoryDetails().then()
                .spec(jsonResponse(200)).extract().as(ProductCategory[].class);

        Set<String> expectedSlugs = Set.copyOf(Arrays.asList(categorySlugs));
        Set<String> actualSlugs = Arrays.stream(categoryDetails)
                .map(ProductCategory::slug)
                .collect(Collectors.toSet());
        assertAll(
                () -> assertEquals(categorySlugs.length, expectedSlugs.size()),
                () -> assertEquals(categoryDetails.length, actualSlugs.size()),
                () -> assertEquals(expectedSlugs, actualSlugs)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"beauty", "furniture", "smartphones"})
    void filtersProductsByCategory(String category) {
        ProductsResponse response = client.getProductsByCategory(category).then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        assertFalse(response.products().isEmpty());
        for (Product product : response.products()) {
            assertEquals(category, product.category(), "Category for product " + product.id());
        }
    }

    @Test
    void paginatesProductsWithinCategory() {
        String category = "beauty";
        ProductsResponse response = client.getProductsByCategory(category, 2, 1).then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        assertAll(
                () -> assertEquals(2, response.limit()),
                () -> assertEquals(1, response.skip()),
                () -> assertEquals(2, response.products().size()),
                () -> assertTrue(response.total() >= response.skip() + response.limit())
        );
        for (Product product : response.products()) {
            assertEquals(category, product.category(), "Category for product " + product.id());
        }
    }

    @Test
    void returnsOnlySelectedFieldsForCategoryProducts() {
        List<Map<String, Object>> products = client.getProductCategoryFields("beauty", "title", "price").then()
                .spec(jsonResponse(200)).extract().jsonPath().getList("products");

        assertFalse(products.isEmpty());
        for (Map<String, Object> product : products) {
            assertEquals(Set.of("id", "title", "price"), product.keySet());
            assertFalse(((String) product.get("title")).isBlank());
            assertTrue(product.get("price") instanceof Number);
        }
    }

    @Test
    void paginatesSelectedFieldsWithinCategory() {
        List<Map<String, Object>> products = client.getProductCategoryFields("beauty", 2, 1, "title", "price").then()
                .spec(jsonResponse(200))
                .body("limit", equalTo(2))
                .body("skip", equalTo(1))
                .extract().jsonPath().getList("products");

        assertEquals(2, products.size());
        for (Map<String, Object> product : products) {
            assertEquals(Set.of("id", "title", "price"), product.keySet());
            assertFalse(((String) product.get("title")).isBlank());
            assertTrue(product.get("price") instanceof Number);
        }
    }

    @Test
    void preservesCategoryPageIdentityForSelectedFields() {
        ProductsResponse fullPage = client.getProductsByCategory("beauty", 2, 1).then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);
        List<Map<String, Object>> projectedPage = client.getProductCategoryFields("beauty", 2, 1, "title", "price").then()
                .spec(jsonResponse(200)).extract().jsonPath().getList("products");

        List<Integer> fullPageIds = fullPage.products().stream().map(Product::id).toList();
        List<Integer> projectedPageIds = projectedPage.stream()
                .map(product -> ((Number) product.get("id")).intValue())
                .toList();
        assertEquals(fullPageIds, projectedPageIds);
    }

    @Test
    void returnsMinimalProjectionForCategoryProducts() {
        List<Map<String, Object>> products = client.getProductCategoryFields("beauty", "title").then()
                .spec(jsonResponse(200)).extract().jsonPath().getList("products");

        assertFalse(products.isEmpty());
        for (Map<String, Object> product : products) {
            assertEquals(Set.of("id", "title"), product.keySet());
            assertFalse(((String) product.get("title")).isBlank());
        }
    }

    @Test
    void returnsEmptyResultsForUnknownSearchTerm() {
        ProductsResponse response = client.searchProducts("qa-no-product-7a6d921e").then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        assertEquals(0, response.total());
        assertTrue(response.products().isEmpty());
    }

    @Test
    void paginatesProductSearchResults() {
        ProductsResponse response = client.searchProducts("phone", 3, 1).then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        assertAll(
                () -> assertEquals(3, response.limit()),
                () -> assertEquals(1, response.skip()),
                () -> assertEquals(3, response.products().size()),
                () -> assertTrue(response.total() >= response.skip() + response.limit())
        );
    }

    @Test
    void sortsProductSearchResultsByPriceInAscendingOrder() {
        ProductsResponse response = client.searchProductsSortedBy("phone", 10, "price", "asc").then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        assertEquals(10, response.products().size());
        for (int index = 1; index < response.products().size(); index++) {
            BigDecimal previousPrice = response.products().get(index - 1).price();
            BigDecimal currentPrice = response.products().get(index).price();
            assertTrue(previousPrice.compareTo(currentPrice) <= 0,
                    "Search results are not sorted at index " + index);
        }
    }

    @Test
    void preservesSortedSearchPageIdentity() {
        ProductsResponse fullResult = client.searchProductsSortedBy("phone", 10, "price", "asc").then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);
        ProductsResponse pagedResult = client.searchProductsSortedBy("phone", 3, 2, "price", "asc").then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        List<Integer> expectedIds = fullResult.products().subList(2, 5).stream().map(Product::id).toList();
        List<Integer> actualIds = pagedResult.products().stream().map(Product::id).toList();
        assertEquals(expectedIds, actualIds);
    }

    @Test
    void sortsProductSearchResultsByTitleInDescendingOrder() {
        ProductsResponse response = client.searchProductsSortedBy("phone", 5, "title", "desc").then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);

        assertEquals(5, response.products().size());
        for (int index = 1; index < response.products().size(); index++) {
            String previousTitle = response.products().get(index - 1).title();
            String currentTitle = response.products().get(index).title();
            assertTrue(previousTitle.compareToIgnoreCase(currentTitle) >= 0,
                    "Search result titles are not sorted at index " + index);
        }
    }

    @Test
    void returnsSelectedFieldsForProductSearch() {
        List<Map<String, Object>> products = client.searchProductFields("phone", 3, 0, "title", "price").then()
                .spec(jsonResponse(200))
                .body("limit", equalTo(3))
                .body("skip", equalTo(0))
                .extract().jsonPath().getList("products");

        assertEquals(3, products.size());
        for (Map<String, Object> product : products) {
            assertEquals(Set.of("id", "title", "price"), product.keySet());
            assertFalse(((String) product.get("title")).isBlank());
            assertTrue(product.get("price") instanceof Number);
        }
    }

    @Test
    void preservesSearchPageIdentityForSelectedFields() {
        ProductsResponse fullPage = client.searchProducts("phone", 3, 1).then()
                .spec(jsonResponse(200)).extract().as(ProductsResponse.class);
        List<Map<String, Object>> projectedPage = client.searchProductFields("phone", 3, 1, "title", "price").then()
                .spec(jsonResponse(200)).extract().jsonPath().getList("products");

        List<Integer> fullPageIds = fullPage.products().stream().map(Product::id).toList();
        List<Integer> projectedPageIds = projectedPage.stream()
                .map(product -> ((Number) product.get("id")).intValue())
                .toList();
        assertEquals(fullPageIds, projectedPageIds);
    }

    @Test
    void echoesCreatedProductWithoutPersistingIt() {
        ProductRequest request = JsonFiles.read("/testdata/products/new-product.json", ProductRequest.class);
        Product product = client.createProduct(request).then()
                .spec(jsonResponse(201)).extract().as(Product.class);

        assertTrue(product.id() > 0);
        assertProductMatches(request, product);
        client.getProduct(product.id()).then().spec(jsonResponse(404));
    }

    @Test
    void echoesUpdatedProductWithoutChangingStoredData() {
        Product original = client.getProduct(1).then()
                .spec(jsonResponse(200)).extract().as(Product.class);
        ProductRequest request = JsonFiles.read("/testdata/products/new-product.json", ProductRequest.class);

        Product updated = client.updateProduct(1, request).then()
                .spec(jsonResponse(200)).extract().as(Product.class);

        assertEquals(1, updated.id());
        assertProductMatches(request, updated);
        Product stored = client.getProduct(1).then()
                .spec(jsonResponse(200)).extract().as(Product.class);
        assertEquals(original, stored);
    }

    @Test
    void marksDeletedProductWithoutRemovingIt() {
        client.deleteProduct(1).then().spec(jsonResponse(200))
                .body("id", equalTo(1)).body("isDeleted", equalTo(true));
        client.getProduct(1).then().spec(jsonResponse(200)).body("id", equalTo(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 99999999})
    void rejectsMissingProduct(int id) {
        client.getProduct(id).then().spec(jsonResponse(404))
                .body("message", equalTo("Product with id '" + id + "' not found"));
    }

    @Test
    void rejectsUnknownEndpoint() {
        given().spec(ApiSpecifications.request()).get("/qa-unknown-endpoint")
                .then().statusCode(404);
    }

    private void assertProductMatches(ProductRequest expected, Product actual) {
        assertEquals(expected.title(), actual.title());
        assertEquals(expected.description(), actual.description());
        assertEquals(0, expected.price().compareTo(actual.price()));
        assertEquals(expected.category(), actual.category());
    }
}

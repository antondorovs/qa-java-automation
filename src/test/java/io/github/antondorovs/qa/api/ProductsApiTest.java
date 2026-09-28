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

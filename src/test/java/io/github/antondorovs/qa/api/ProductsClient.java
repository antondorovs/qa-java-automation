package io.github.antondorovs.qa.api;

import io.github.antondorovs.qa.models.ProductRequest;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class ProductsClient {
    public Response getProduct(int id) {
        return given().spec(ApiSpecifications.request()).get("/products/{id}", id);
    }

    public Response getProductFields(int id, String... fields) {
        return given().spec(ApiSpecifications.request())
                .queryParam("select", String.join(",", fields))
                .get("/products/{id}", id);
    }

    public Response listProducts(int limit, int skip) {
        return given().spec(ApiSpecifications.request())
                .queryParam("limit", limit).queryParam("skip", skip).get("/products");
    }

    public Response listProductFields(int limit, int skip, String... fields) {
        return given().spec(ApiSpecifications.request())
                .queryParam("limit", limit)
                .queryParam("skip", skip)
                .queryParam("select", String.join(",", fields))
                .get("/products");
    }

    public Response listProductsSortedBy(int limit, String field, String order) {
        return given().spec(ApiSpecifications.request())
                .queryParam("limit", limit)
                .queryParam("sortBy", field)
                .queryParam("order", order)
                .get("/products");
    }

    public Response listProductCategories() {
        return given().spec(ApiSpecifications.request()).get("/products/category-list");
    }

    public Response listProductCategoryDetails() {
        return given().spec(ApiSpecifications.request()).get("/products/categories");
    }

    public Response searchProducts(String query) {
        return given().spec(ApiSpecifications.request())
                .queryParam("q", query).get("/products/search");
    }

    public Response searchProducts(String query, int limit, int skip) {
        return given().spec(ApiSpecifications.request())
                .queryParam("q", query)
                .queryParam("limit", limit)
                .queryParam("skip", skip)
                .get("/products/search");
    }

    public Response getProductsByCategory(String category) {
        return given().spec(ApiSpecifications.request()).get("/products/category/{category}", category);
    }

    public Response getProductCategoryFields(String category, String... fields) {
        return given().spec(ApiSpecifications.request())
                .queryParam("select", String.join(",", fields))
                .get("/products/category/{category}", category);
    }

    public Response getProductsByCategory(String category, int limit, int skip) {
        return given().spec(ApiSpecifications.request())
                .queryParam("limit", limit)
                .queryParam("skip", skip)
                .get("/products/category/{category}", category);
    }

    public Response createProduct(ProductRequest product) {
        return given().spec(ApiSpecifications.request()).body(product).post("/products/add");
    }

    public Response updateProduct(int id, ProductRequest product) {
        return given().spec(ApiSpecifications.request()).body(product).patch("/products/{id}", id);
    }

    public Response deleteProduct(int id) {
        return given().spec(ApiSpecifications.request()).delete("/products/{id}", id);
    }
}

package io.github.antondorovs.qa.utils;

import io.github.antondorovs.qa.models.ProductRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Tag("api")
class JsonFilesTest {
    @Test
    void readsProductFixture() {
        ProductRequest product = JsonFiles.read("/testdata/products/new-product.json", ProductRequest.class);

        assertEquals("QA Travel Backpack", product.title());
        assertEquals(new BigDecimal("49.95"), product.price());
        assertEquals("mens-bags", product.category());
    }

    @Test
    void rejectsAdditionalJsonValues() {
        String resource = "/testdata/invalid/trailing-values.json";

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> JsonFiles.read(resource, ProductRequest.class));

        assertTrue(exception.getMessage().contains(resource));
        assertNotNull(exception.getCause());
    }

    @Test
    void rejectsDuplicateProductFields() {
        String resource = "/testdata/invalid/duplicate-fields.json";

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> JsonFiles.read(resource, ProductRequest.class));

        assertTrue(exception.getMessage().contains(resource));
        assertNotNull(exception.getCause());
    }

    @Test
    void identifiesMissingFixture() {
        String resource = "/testdata/missing.json";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JsonFiles.read(resource, ProductRequest.class));

        assertTrue(exception.getMessage().contains(resource));
    }
}

package io.github.antondorovs.qa.ui;

import io.github.antondorovs.qa.ui.pages.CartPage;
import io.github.antondorovs.qa.ui.pages.InventoryPage;
import io.github.antondorovs.qa.ui.pages.LoginPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartTest extends BaseUiTest {
    @Test
    void addsAndRemovesProduct() {
        InventoryPage inventory = new LoginPage().openPage().loginAsStandardUser();
        inventory.addToCart("Sauce Labs Backpack");
        inventory.shouldHaveCartCount(1);

        CartPage cart = inventory.openCart();
        cart.shouldContain("Sauce Labs Backpack", "$29.99");
        cart.removeProduct("Sauce Labs Backpack");
        cart.shouldBeEmpty();
    }

    @Test
    void removesOnlySelectedProductFromMultiItemCart() {
        InventoryPage inventory = new LoginPage().openPage().loginAsStandardUser();
        inventory.addToCart("Sauce Labs Backpack");
        inventory.addToCart("Sauce Labs Bike Light");
        inventory.shouldHaveCartCount(2);

        CartPage cart = inventory.openCart();
        cart.shouldHaveItemCount(2);
        cart.shouldContain("Sauce Labs Backpack", "$29.99");
        cart.shouldContain("Sauce Labs Bike Light", "$9.99");

        cart.removeProduct("Sauce Labs Backpack");

        cart.shouldHaveItemCount(1);
        cart.shouldContain("Sauce Labs Bike Light", "$9.99");
        cart.removeProduct("Sauce Labs Bike Light");
        cart.shouldBeEmpty();
    }

    @ParameterizedTest(name = "price sort {0}")
    @CsvSource({"lohi, false", "hilo, true"})
    void sortsProductsByPrice(String option, boolean descending) {
        InventoryPage inventory = new LoginPage().openPage().loginAsStandardUser();
        List<BigDecimal> expected = new ArrayList<>(inventory.productPrices());
        Collections.sort(expected);
        if (descending) {
            Collections.reverse(expected);
        }

        inventory.sortBy(option);

        assertEquals(expected, inventory.productPrices());
    }
}

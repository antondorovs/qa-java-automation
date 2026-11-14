package io.github.antondorovs.qa.ui.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class InventoryPage {
    private final SelenideElement title = $("[data-test='title']");
    private final ElementsCollection products = $$("[data-test='inventory-item']");
    private final ElementsCollection prices = $$("[data-test='inventory-item-price']");

    public void shouldBeOpen() {
        title.shouldHave(exactText("Products"));
        products.shouldHave(sizeGreaterThan(0));
    }

    public void addToCart(String productName) {
        products.findBy(text(productName)).$("button").click();
    }

    public void shouldHaveCartCount(int count) {
        $("[data-test='shopping-cart-badge']").shouldHave(exactText(String.valueOf(count)));
    }

    public CartPage openCart() {
        $("[data-test='shopping-cart-link']").click();
        CartPage cart = new CartPage();
        cart.shouldBeOpen();
        return cart;
    }

    public void sortBy(String value) {
        $("[data-test='product-sort-container']").selectOptionByValue(value);
    }

    public List<String> productNames() {
        return $$("[data-test='inventory-item-name']").shouldHave(sizeGreaterThan(1)).texts();
    }

    public List<BigDecimal> productPrices() {
        prices.shouldHave(sizeGreaterThan(1));
        List<BigDecimal> values = new ArrayList<>();
        for (String price : prices.texts()) {
            values.add(new BigDecimal(price.replace("$", "")));
        }
        return values;
    }
}

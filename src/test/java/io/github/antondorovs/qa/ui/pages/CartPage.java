package io.github.antondorovs.qa.ui.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class CartPage {
    private final ElementsCollection items = $$("[data-test='inventory-item']");

    public void shouldBeOpen() {
        $("[data-test='title']").shouldHave(exactText("Your Cart"));
    }

    public void shouldContain(String productName, String price) {
        SelenideElement item = items.findBy(text(productName)).shouldBe(visible);
        item.$("[data-test='inventory-item-name']").shouldHave(exactText(productName));
        item.$("[data-test='inventory-item-price']").shouldHave(exactText(price));
        item.$("[data-test='item-quantity']").shouldHave(exactText("1"));
    }

    public void removeProduct(String productName) {
        items.findBy(text(productName)).$("button").click();
    }

    public void shouldHaveItemCount(int count) {
        items.shouldHave(size(count));
        if (count == 0) {
            $("[data-test='shopping-cart-badge']").shouldNotBe(visible);
        } else {
            $("[data-test='shopping-cart-badge']").shouldHave(exactText(String.valueOf(count)));
        }
    }

    public void shouldBeEmpty() {
        items.shouldHave(size(0));
        $("[data-test='shopping-cart-badge']").shouldNotBe(visible);
    }

    public CheckoutPage checkout() {
        $("[data-test='checkout']").click();
        CheckoutPage checkout = new CheckoutPage();
        checkout.shouldShowInformationForm();
        return checkout;
    }
}

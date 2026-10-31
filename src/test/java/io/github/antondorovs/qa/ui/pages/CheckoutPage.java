package io.github.antondorovs.qa.ui.pages;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class CheckoutPage {
    public void shouldShowInformationForm() {
        $("[data-test='title']").shouldHave(exactText("Checkout: Your Information"));
    }

    public void submitCustomer(String firstName, String lastName, String postalCode) {
        $("[data-test='firstName']").setValue(firstName);
        $("[data-test='lastName']").setValue(lastName);
        $("[data-test='postalCode']").setValue(postalCode);
        $("[data-test='continue']").click();
    }

    public void shouldShowError(String message) {
        $("[data-test='error']").shouldBe(visible).shouldHave(exactText(message));
        shouldShowInformationForm();
    }

    public void shouldShowOrder(String productName, String subtotal, String tax, String total) {
        $("[data-test='title']").shouldHave(exactText("Checkout: Overview"));
        $("[data-test='inventory-item-name']").shouldHave(exactText(productName));
        $("[data-test='subtotal-label']").shouldHave(exactText("Item total: $" + subtotal));
        $("[data-test='tax-label']").shouldHave(exactText("Tax: $" + tax));
        $("[data-test='total-label']").shouldHave(exactText("Total: $" + total));
    }

    public void finishOrder() {
        $("[data-test='finish']").click();
    }

    public InventoryPage cancelCheckout() {
        $("[data-test='cancel']").click();
        InventoryPage inventory = new InventoryPage();
        inventory.shouldBeOpen();
        return inventory;
    }

    public void shouldShowConfirmation() {
        $("[data-test='complete-header']").shouldHave(exactText("Thank you for your order!"));
        $("[data-test='shopping-cart-badge']").shouldNotBe(visible);
    }
}

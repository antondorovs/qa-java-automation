package io.github.antondorovs.qa.ui;

import io.github.antondorovs.qa.ui.pages.CartPage;
import io.github.antondorovs.qa.ui.pages.CheckoutPage;
import io.github.antondorovs.qa.ui.pages.InventoryPage;
import io.github.antondorovs.qa.ui.pages.LoginPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CheckoutTest extends BaseUiTest {
    @Test
    void completesOrderWithExpectedTotal() {
        CheckoutPage checkout = openCheckout();
        checkout.submitCustomer("Alex", "Morgan", "10001");
        checkout.shouldShowOrder("Sauce Labs Backpack", "29.99", "2.40", "32.39");

        checkout.finishOrder();

        checkout.shouldShowConfirmation();
    }

    @Test
    void cancellationPreservesCartProduct() {
        CheckoutPage checkout = openCheckout();
        checkout.submitCustomer("Alex", "Morgan", "10001");
        checkout.shouldShowOrder("Sauce Labs Backpack", "29.99", "2.40", "32.39");

        InventoryPage inventory = checkout.cancelCheckout();

        inventory.shouldHaveCartCount(1);
        CartPage cart = inventory.openCart();
        cart.shouldHaveItemCount(1);
        cart.shouldContain("Sauce Labs Backpack", "$29.99");
    }

    @ParameterizedTest(name = "required field: {3}")
    @CsvSource({
            "'', Morgan, 10001, Error: First Name is required",
            "Alex, '', 10001, Error: Last Name is required",
            "Alex, Morgan, '', Error: Postal Code is required"
    })
    void requiresCustomerDetails(String firstName, String lastName, String postalCode, String message) {
        CheckoutPage checkout = openCheckout();

        checkout.submitCustomer(firstName, lastName, postalCode);

        checkout.shouldShowError(message);
    }

    private CheckoutPage openCheckout() {
        InventoryPage inventory = new LoginPage().openPage().loginAsStandardUser();
        inventory.addToCart("Sauce Labs Backpack");
        return inventory.openCart().checkout();
    }
}

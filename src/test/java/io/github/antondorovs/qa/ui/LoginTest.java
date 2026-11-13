package io.github.antondorovs.qa.ui;

import io.github.antondorovs.qa.ui.pages.LoginPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LoginTest extends BaseUiTest {
    @Test
    void opensCatalogAfterLogin() {
        new LoginPage().openPage().loginAsStandardUser().shouldBeOpen();
    }

    @ParameterizedTest(name = "login rejected for {0}")
    @CsvSource({
            "standard_user, wrong-password, Epic sadface: Username and password do not match any user in this service",
            "locked_out_user, secret_sauce, 'Epic sadface: Sorry, this user has been locked out.'"
    })
    void rejectsInvalidLogin(String username, String password, String message) {
        LoginPage login = new LoginPage().openPage();
        login.loginAs(username, password);
        login.shouldShowError(message);
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource({
            "'', secret_sauce, Epic sadface: Username is required",
            "standard_user, '', Epic sadface: Password is required"
    })
    void requiresBothLoginFields(String username, String password, String message) {
        LoginPage login = new LoginPage().openPage();
        login.loginAs(username, password);
        login.shouldShowError(message);
    }
}

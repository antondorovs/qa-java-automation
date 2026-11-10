package io.github.antondorovs.qa.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@Tag("config")
@ResourceLock(SYSTEM_PROPERTIES)
class TestConfigTest {
    private String originalTimeout;
    private String originalHeadless;

    @BeforeEach
    void rememberOverrides() {
        originalTimeout = System.getProperty("ui.timeout");
        originalHeadless = System.getProperty("headless");
    }

    @AfterEach
    void restoreOverrides() {
        restoreProperty("ui.timeout", originalTimeout);
        restoreProperty("headless", originalHeadless);
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @ParameterizedTest
    @CsvSource({"' TrUe ', true", "' FaLsE ', false"})
    void acceptsHeadlessOverridesRegardlessOfCaseAndWhitespace(String value, boolean expected) {
        System.setProperty("headless", value);

        assertEquals(expected, TestConfig.headless());
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   ", "yes", "0"})
    void rejectsMalformedHeadlessOverrides(String value) {
        System.setProperty("headless", value);

        IllegalArgumentException exception = assertThrowsExactly(IllegalArgumentException.class, TestConfig::headless);
        assertEquals("HEADLESS must be true or false", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 10000, Long.MAX_VALUE})
    void acceptsPositiveTimeouts(long timeout) {
        System.setProperty("ui.timeout", "  " + timeout + "  ");

        assertEquals(timeout, TestConfig.uiTimeout());
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   ", "abc", "1.5", "9223372036854775808"})
    void identifiesMalformedTimeoutConfiguration(String value) {
        System.setProperty("ui.timeout", value);

        IllegalArgumentException exception = assertThrowsExactly(IllegalArgumentException.class, TestConfig::uiTimeout);
        assertTrue(exception.getMessage().contains("UI_TIMEOUT"));
        assertTrue(exception.getMessage().contains("positive whole number"));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void rejectsNonPositiveTimeouts(long timeout) {
        System.setProperty("ui.timeout", Long.toString(timeout));

        IllegalArgumentException exception = assertThrowsExactly(IllegalArgumentException.class, TestConfig::uiTimeout);
        assertTrue(exception.getMessage().contains("UI_TIMEOUT"));
        assertTrue(exception.getMessage().contains("positive"));
    }
}

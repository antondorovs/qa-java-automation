package io.github.antondorovs.qa.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
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

    @BeforeEach
    void rememberTimeoutOverride() {
        originalTimeout = System.getProperty("ui.timeout");
    }

    @AfterEach
    void restoreTimeoutOverride() {
        if (originalTimeout == null) {
            System.clearProperty("ui.timeout");
        } else {
            System.setProperty("ui.timeout", originalTimeout);
        }
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

package io.github.antondorovs.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class TestConfig {
    private static final Properties PROPERTIES = loadProperties();

    private TestConfig() {
    }

    public static String get(String key, String environmentVariable) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(environmentVariable);
        }
        if (value == null) {
            value = PROPERTIES.getProperty(key);
        }
        if (value == null) {
            throw new IllegalArgumentException("Missing configuration: " + key);
        }
        return value.trim();
    }

    public static boolean headless() {
        String value = get("headless", "HEADLESS");
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("HEADLESS must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    public static long uiTimeout() {
        long timeout;
        try {
            timeout = Long.parseLong(get("ui.timeout", "UI_TIMEOUT"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("UI_TIMEOUT must be a positive whole number (milliseconds)", exception);
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("UI_TIMEOUT must be positive (milliseconds)");
        }
        return timeout;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream defaults = TestConfig.class.getResourceAsStream("/test.properties")) {
            if (defaults == null) {
                throw new IllegalStateException("test.properties was not found");
            }
            properties.load(defaults);
            Path localFile = Path.of("local.properties");
            if (Files.exists(localFile)) {
                try (InputStream local = Files.newInputStream(localFile)) {
                    properties.load(local);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read test configuration", exception);
        }
        return properties;
    }
}

package io.github.antondorovs.qa.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

public final class JsonFiles {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private JsonFiles() {
    }

    public static <T> T read(String resource, Class<T> type) {
        try (InputStream stream = JsonFiles.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalArgumentException("Test data was not found: " + resource);
            }
            return MAPPER.readValue(stream, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read test data: " + resource, exception);
        }
    }
}

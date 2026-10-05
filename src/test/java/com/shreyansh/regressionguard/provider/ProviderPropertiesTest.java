package com.shreyansh.regressionguard.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ProviderPropertiesTest {

    @Test
    void trailingSlashIsRemovedFromBaseUrl() {
        var props = new ProviderProperties("https://api.example.com/v1/", "key", "m", null);
        assertEquals("https://api.example.com/v1", props.baseUrl());
    }

    @Test
    void missingTimeoutDefaultsToThirtySeconds() {
        var props = new ProviderProperties("https://api.example.com/v1", "key", "m", null);
        assertEquals(Duration.ofSeconds(30), props.timeout());
    }

    @Test
    void toStringNeverShowsTheKey() {
        var props = new ProviderProperties("https://api.example.com/v1", "secret-key-123", "m", null);
        assertFalse(props.toString().contains("secret-key-123"));
        assertTrue(props.toString().contains("****"));
    }

    @Test
    void blankModelIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProviderProperties("https://api.example.com/v1", "key", " ", null));
    }
}
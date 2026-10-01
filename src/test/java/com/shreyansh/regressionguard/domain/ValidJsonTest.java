package com.shreyansh.regressionguard.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidJsonTest {

    private final ValidJson rule = new ValidJson();

    @Test
    void objectPasses() {
        assertTrue(rule.check("{\"status\": \"ok\"}").passed());
    }

    @Test
    void arrayPasses() {
        assertTrue(rule.check("[1, 2, 3]").passed());
    }

    @Test
    void brokenJsonFails() {
        assertFalse(rule.check("{\"status\": ").passed());
    }

    @Test
    void plainTextFails() {
        assertFalse(rule.check("Sure! Here is the JSON you asked for").passed());
    }

    @Test
    void trailingTextFails() {
        assertFalse(rule.check("{} and some extra words").passed());
    }

    @Test
    void markdownFencedJsonFails() {
        assertFalse(rule.check("```json\n{\"a\": 1}\n```").passed());
    }

    @Test
    void emptyOrNullFails() {
        assertFalse(rule.check("").passed());
        assertFalse(rule.check(null).passed());
    }
}
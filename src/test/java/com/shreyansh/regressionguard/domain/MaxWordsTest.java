package com.shreyansh.regressionguard.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaxWordsTest {

    private final MaxWords rule = new MaxWords(3);

    @Test
    void underLimitPasses() {
        assertTrue(rule.check("one two").passed());
    }

    @Test
    void exactlyAtLimitPasses() {
        assertTrue(rule.check("one two three").passed());
    }

    @Test
    void overLimitFailsWithTheCount() {
        RuleResult result = rule.check("one two three four");
        assertFalse(result.passed());
        assertEquals("4 words, limit is 3", result.message());
    }

    @Test
    void extraSpacesAndNewlinesDoNotAddWords() {
        assertTrue(rule.check("  one\n\n two   three  ").passed());
    }

    @Test
    void emptyResponseIsZeroWords() {
        assertTrue(rule.check("").passed());
    }

    @Test
    void limitBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MaxWords(0));
    }
}
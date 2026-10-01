package com.shreyansh.regressionguard.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RequiredFieldsTest {

    private final RequiredFields rule = new RequiredFields(List.of("status", "reason"));

    @Test
    void allFieldsPresentPasses() {
        assertTrue(rule.check("{\"status\": \"ok\", \"reason\": \"fine\", \"extra\": 1}").passed());
    }

    @Test
    void missingFieldFailsAndNamesIt() {
        RuleResult result = rule.check("{\"status\": \"ok\"}");
        assertFalse(result.passed());
        assertTrue(result.message().contains("reason"));
    }

    @Test
    void nullValueCountsAsMissing() {
        assertFalse(rule.check("{\"status\": \"ok\", \"reason\": null}").passed());
    }

    @Test
    void arrayIsNotAnObject() {
        RuleResult result = rule.check("[{\"status\": \"ok\", \"reason\": \"x\"}]");
        assertFalse(result.passed());
        assertTrue(result.message().contains("object"));
    }

    @Test
    void invalidJsonFails() {
        assertFalse(rule.check("not json").passed());
    }

    @Test
    void nameListsTheFields() {
        assertEquals("RequiredFields[status, reason]", rule.name());
    }

    @Test
    void emptyFieldListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RequiredFields(List.of()));
    }
}
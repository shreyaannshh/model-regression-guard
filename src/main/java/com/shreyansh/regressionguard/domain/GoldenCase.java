package com.shreyansh.regressionguard.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A prompt worth guarding.
 *
 * @param id            stable across baselines and runs
 * @param prompt        the exact text sent to the provider
 * @param propertyRules deterministic checks; an empty list means similarity only
 * @param createdAt     when the case was registered
 */
public record GoldenCase(String id, String prompt, List<PropertyRule> propertyRules, Instant createdAt) {

    public GoldenCase {
        requireNonBlank(id, "id");
        requireNonBlank(prompt, "prompt");
        Objects.requireNonNull(createdAt, "createdAt");
        // Defensive copy: callers can't change a case's rules after it is created.
        propertyRules = propertyRules == null ? List.of() : List.copyOf(propertyRules);
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}

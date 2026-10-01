package com.shreyansh.regressionguard.domain;

import java.util.List;
import tools.jackson.core.JacksonException;

/**
 * Passes when the response is a JSON object and every listed top-level field is present and not null.
 */
public record RequiredFields(List<String> fields) implements PropertyRule {

    public RequiredFields {
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("fields must not be empty");
        }
        fields = List.copyOf(fields);
    }

    @Override
    public String name() {
        return "RequiredFields" + fields;
    }

    @Override
    public RuleResult check(String response) {
        if (response == null || response.isBlank()) {
            return RuleResult.fail(name(), "response is empty");
        }
        try {
            var root = Json.MAPPER.readTree(response);
            if (!root.isObject()) {
                return RuleResult.fail(name(), "expected a JSON object");
            }
            List<String> missing = fields.stream()
                    .filter(field -> !root.hasNonNull(field))
                    .toList();
            if (missing.isEmpty()) {
                return RuleResult.pass(name());
            }
            return RuleResult.fail(name(), "missing or null: " + missing);
        } catch (JacksonException e) {
            return RuleResult.fail(name(), "not valid JSON, so fields can't be checked");
        }
    }
}
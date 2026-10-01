package com.shreyansh.regressionguard.domain;

import tools.jackson.core.JacksonException;

/**
 * Passes when the whole response is one valid JSON value.
 * Strict on purpose: any text around the JSON fails, including ```json fences.
 */
public record ValidJson() implements PropertyRule {

    @Override
    public String name() {
        return "ValidJson";
    }

    @Override
    public RuleResult check(String response) {
        if (response == null || response.isBlank()) {
            return RuleResult.fail(name(), "response is empty");
        }
        try {
            Json.MAPPER.readTree(response);
            return RuleResult.pass(name());
        } catch (JacksonException e) {
            return RuleResult.fail(name(), "not valid JSON");
        }
    }
}
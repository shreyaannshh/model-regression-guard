package com.shreyansh.regressionguard.domain;

import java.util.Objects;

/**
 * The outcome of one property rule against one response.
 *
 * @param ruleName which rule ran, for example "MaxWords(200)"
 * @param passed   whether the response satisfied the rule
 * @param message  a short human-readable reason; empty when the rule passed
 */
public record RuleResult(String ruleName, boolean passed, String message) {

    public RuleResult {
        Objects.requireNonNull(ruleName, "ruleName");
        message = message == null ? "" : message;
    }

    public static RuleResult pass(String ruleName) {
        return new RuleResult(ruleName, true, "");
    }

    public static RuleResult fail(String ruleName, String message) {
        return new RuleResult(ruleName, false, message);
    }
}

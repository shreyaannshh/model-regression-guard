package com.shreyansh.regressionguard.domain;

/**
 * Passes when the response has at most {@code limit} words.
 * A word is anything between whitespace, the way a person would count.
 */
public record MaxWords(int limit) implements PropertyRule {

    public MaxWords {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1, was " + limit);
        }
    }

    @Override
    public String name() {
        return "MaxWords(" + limit + ")";
    }

    @Override
    public RuleResult check(String response) {
        if (response == null) {
            return RuleResult.fail(name(), "no response");
        }
        int count = countWords(response);
        if (count <= limit) {
            return RuleResult.pass(name());
        }
        return RuleResult.fail(name(), count + " words, limit is " + limit);
    }

    static int countWords(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\s+").length;
    }
}
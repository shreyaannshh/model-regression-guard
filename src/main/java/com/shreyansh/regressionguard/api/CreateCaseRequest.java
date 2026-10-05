package com.shreyansh.regressionguard.api;

import com.shreyansh.regressionguard.domain.GoldenCase;
import com.shreyansh.regressionguard.domain.PropertyRule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Body of POST /cases. */
public record CreateCaseRequest(String id, String prompt, List<RuleRequest> rules) {

    // Ids appear in URLs and run reports, so keep them short and URL-safe.
    private static final Pattern ID_FORMAT = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    GoldenCase toDomain(Instant now) {
        if (id == null || !ID_FORMAT.matcher(id).matches()) {
            throw new IllegalArgumentException("id must be 1 to 64 letters, digits, hyphens or underscores");
        }
        List<PropertyRule> propertyRules = new ArrayList<>();
        if (rules != null) {
            for (RuleRequest rule : rules) {
                if (rule == null) {
                    throw new IllegalArgumentException("rules must not contain null entries");
                }
                propertyRules.add(rule.toRule());
            }
        }
        return new GoldenCase(id, prompt, propertyRules, now);
    }
}
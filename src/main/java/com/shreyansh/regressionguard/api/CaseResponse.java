package com.shreyansh.regressionguard.api;

import com.shreyansh.regressionguard.domain.GoldenCase;
import com.shreyansh.regressionguard.domain.PropertyRule;
import java.time.Instant;
import java.util.List;

/** A case as the API returns it. Rules are shown by name, for example "MaxWords(200)". */
public record CaseResponse(String id, String prompt, List<String> rules, Instant createdAt) {

    static CaseResponse from(GoldenCase goldenCase) {
        List<String> ruleNames = goldenCase.propertyRules().stream()
                .map(PropertyRule::name)
                .toList();
        return new CaseResponse(goldenCase.id(), goldenCase.prompt(), ruleNames, goldenCase.createdAt());
    }
}
package com.shreyansh.regressionguard.domain;

import java.util.List;
import java.util.Objects;

/**
 * The outcome of one case inside one run.
 *
 * @param caseId        the {@link GoldenCase} replayed
 * @param response      what the provider returned; null when the verdict is ERROR
 * @param modelReported the model name the provider reported for this call; may be null
 * @param ruleResults   every rule's outcome, passed or not, so BROKEN can say which rule failed
 * @param similarity    score against the baseline; null when there was no baseline or no response.
 *                      Still stored for BROKEN cases, but not used in their verdict
 * @param verdict       the single outcome
 */
public record CaseResult(
        String caseId,
        String response,
        String modelReported,
        List<RuleResult> ruleResults,
        Double similarity,
        Verdict verdict) {

    public CaseResult {
        Objects.requireNonNull(caseId, "caseId");
        Objects.requireNonNull(verdict, "verdict");
        ruleResults = ruleResults == null ? List.of() : List.copyOf(ruleResults);
    }
}

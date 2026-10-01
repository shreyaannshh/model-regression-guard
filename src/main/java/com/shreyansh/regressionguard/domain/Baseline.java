package com.shreyansh.regressionguard.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * One known-good answer for one case.
 *
 * @param caseId        the {@link GoldenCase} this answers
 * @param response      the text the provider returned at capture time
 * @param capturedAt    when it was captured
 * @param modelReported the model name the provider says it used; may differ from what was requested, may be null
 */
public record Baseline(String caseId, String response, Instant capturedAt, String modelReported) {

    public Baseline {
        Objects.requireNonNull(caseId, "caseId");
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(capturedAt, "capturedAt");
    }
}

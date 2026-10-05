package com.shreyansh.regressionguard.domain;

import java.util.Objects;

/**
 * A case that got no baseline in a capture, and why.
 *
 * @param caseId the {@link GoldenCase} that was skipped
 * @param reason what went wrong, for example "provider returned HTTP 429" or "failed its own rules: ..."
 */
public record SkippedCase(String caseId, String reason) {

    public SkippedCase {
        Objects.requireNonNull(caseId, "caseId");
        Objects.requireNonNull(reason, "reason");
    }
}
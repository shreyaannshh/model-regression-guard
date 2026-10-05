package com.shreyansh.regressionguard.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One capture of known-good answers. Never modified after creation: re-baselining
 * creates a new set, so every past run can still say exactly what it was compared against.
 *
 * @param id             the set's id; runs store it as {@code baselineSetId}
 * @param createdAt      when the capture finished
 * @param modelRequested the model name sent to the provider, for example an alias
 * @param baselines      one baseline per captured case, keyed by case id
 * @param skipped        cases that got no baseline, with the reason. Stored with the set so
 *                       its gaps can always be explained, not only in the capture response
 */
public record BaselineSet(
        String id,
        Instant createdAt,
        String modelRequested,
        Map<String, Baseline> baselines,
        List<SkippedCase> skipped) {

    public BaselineSet {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(modelRequested, "modelRequested");
        baselines = baselines == null ? Map.of() : Map.copyOf(baselines);
        skipped = skipped == null ? List.of() : List.copyOf(skipped);
    }

    /** The baseline for a case, or empty when the case has none in this set (verdict NO_BASELINE). */
    public Optional<Baseline> baselineFor(String caseId) {
        return Optional.ofNullable(baselines.get(caseId));
    }
}
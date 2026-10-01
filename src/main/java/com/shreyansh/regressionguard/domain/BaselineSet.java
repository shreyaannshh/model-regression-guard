package com.shreyansh.regressionguard.domain;

import java.time.Instant;
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
 * @param baselines      one baseline per case, keyed by case id; cases skipped at capture are simply absent
 */
public record BaselineSet(String id, Instant createdAt, String modelRequested, Map<String, Baseline> baselines) {

    public BaselineSet {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(modelRequested, "modelRequested");
        baselines = baselines == null ? Map.of() : Map.copyOf(baselines);
    }

    /** The baseline for a case, or empty when the case has none in this set (verdict NO_BASELINE). */
    public Optional<Baseline> baselineFor(String caseId) {
        return Optional.ofNullable(baselines.get(caseId));
    }
}

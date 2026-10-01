package com.shreyansh.regressionguard.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One replay of every case against one baseline set.
 *
 * @param id             the run's id
 * @param startedAt      when the replay began
 * @param finishedAt     when it ended; gives run duration
 * @param baselineSetId  the set this run was judged against, kept forever so old runs stay readable
 * @param modelRequested the model name sent to the provider
 * @param caseResults    one result per case
 * @param driftScore     mean similarity over compared cases (PASS or DRIFTED); null when none were compared.
 *                       Null, not 0, because 0 would read as "everything drifted"
 * @param comparedCount  how many cases were actually compared against a baseline
 * @param status         the run's overall outcome
 */
public record Run(
        String id,
        Instant startedAt,
        Instant finishedAt,
        String baselineSetId,
        String modelRequested,
        List<CaseResult> caseResults,
        Double driftScore,
        int comparedCount,
        RunStatus status) {

    public Run {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(baselineSetId, "baselineSetId");
        Objects.requireNonNull(status, "status");
        caseResults = caseResults == null ? List.of() : List.copyOf(caseResults);
    }

    /** How many cases got a given verdict. Derived from caseResults, never stored separately, so it can't drift out of sync. */
    public long countOf(Verdict verdict) {
        return caseResults.stream().filter(r -> r.verdict() == verdict).count();
    }
}

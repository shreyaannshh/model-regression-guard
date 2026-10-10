package com.shreyansh.regressionguard.api;

import com.shreyansh.regressionguard.domain.CaseResult;
import com.shreyansh.regressionguard.domain.Run;
import com.shreyansh.regressionguard.domain.RunStatus;
import com.shreyansh.regressionguard.domain.Verdict;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A run as the API returns it: the verdict summary first, the per-case detail last. */
public record RunResponse(
        String id,
        RunStatus status,
        Double driftScore,
        int comparedCount,
        Map<Verdict, Long> counts,
        String baselineSetId,
        String modelRequested,
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        List<CaseResult> caseResults) {

    static RunResponse from(Run run) {
        // Every verdict appears, zeros included, so a reader never has to wonder whether a key is missing.
        Map<Verdict, Long> counts = new EnumMap<>(Verdict.class);
        for (Verdict verdict : Verdict.values()) {
            counts.put(verdict, run.countOf(verdict));
        }
        return new RunResponse(
                run.id(),
                run.status(),
                run.driftScore(),
                run.comparedCount(),
                counts,
                run.baselineSetId(),
                run.modelRequested(),
                run.startedAt(),
                run.finishedAt(),
                Duration.between(run.startedAt(), run.finishedAt()).toMillis(),
                run.caseResults());
    }
}
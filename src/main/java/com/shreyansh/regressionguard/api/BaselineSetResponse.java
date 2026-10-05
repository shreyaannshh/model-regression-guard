package com.shreyansh.regressionguard.api;

import com.shreyansh.regressionguard.domain.Baseline;
import com.shreyansh.regressionguard.domain.BaselineSet;
import com.shreyansh.regressionguard.domain.SkippedCase;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** A baseline set as the API returns it, with counts up front so gaps are obvious at a glance. */
public record BaselineSetResponse(
        String id,
        Instant createdAt,
        String modelRequested,
        boolean active,
        int capturedCount,
        int skippedCount,
        List<BaselineView> baselines,
        List<SkippedCase> skipped) {

    public record BaselineView(String caseId, String response, String modelReported, Instant capturedAt) {
    }

    static BaselineSetResponse from(BaselineSet set, boolean active) {
        List<BaselineView> views = set.baselines().values().stream()
                .sorted(Comparator.comparing(Baseline::caseId))
                .map(b -> new BaselineView(b.caseId(), b.response(), b.modelReported(), b.capturedAt()))
                .toList();
        return new BaselineSetResponse(
                set.id(),
                set.createdAt(),
                set.modelRequested(),
                active,
                views.size(),
                set.skipped().size(),
                views,
                set.skipped());
    }
}
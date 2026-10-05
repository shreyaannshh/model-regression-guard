package com.shreyansh.regressionguard.baseline;

import com.shreyansh.regressionguard.domain.SkippedCase;
import java.util.List;

/**
 * A capture produced no baselines at all, so no set was stored.
 * An empty set, once activated, would make every run INCONCLUSIVE: a setup mistake dressed up as data.
 * The API turns this into 422 and returns the skipped list, so the cause is visible.
 */
public class NothingCapturedException extends RuntimeException {

    private final List<SkippedCase> skipped;

    public NothingCapturedException(String message, List<SkippedCase> skipped) {
        super(message);
        this.skipped = List.copyOf(skipped);
    }

    public List<SkippedCase> skipped() {
        return skipped;
    }
}
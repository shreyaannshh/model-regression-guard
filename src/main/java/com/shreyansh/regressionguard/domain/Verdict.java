package com.shreyansh.regressionguard.domain;

/**
 * The single outcome for one case inside one run.
 *
 * <p>Declared in precedence order: when more than one could apply, the earliest wins.
 * ERROR stops a case before any rule runs. BROKEN outranks NO_BASELINE so a new case
 * that returns invalid output says so instead of hiding behind "no baseline yet".
 */
public enum Verdict {

    /** The provider call itself failed: timeout, 5xx, unreadable body. A fact about the network, not the model. */
    ERROR,

    /** The response failed at least one deterministic property rule. */
    BROKEN,

    /** The active baseline set has no baseline for this case. */
    NO_BASELINE,

    /** Similarity to the baseline fell below the scorer's drift threshold. Behaviour moved; not a claim it got worse. */
    DRIFTED,

    /** Rules passed and similarity is at or above the threshold. */
    PASS;

    /** True when the case was actually compared against a baseline, so its similarity counts toward the drift score. */
    public boolean isCompared() {
        return this == DRIFTED || this == PASS;
    }
}

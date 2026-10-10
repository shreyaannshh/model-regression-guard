package com.shreyansh.regressionguard.domain;

/**
 * The overall outcome of one run. Picked by these rules, first match wins:
 *
 * <ol>
 *   <li>Any case BROKEN → FAILED</li>
 *   <li>Every case ERROR → FAILED (the provider is unusable)</li>
 *   <li>No case compared against a baseline → INCONCLUSIVE</li>
 *   <li>Any case DRIFTED or ERROR → WARNING</li>
 *   <li>Otherwise → PASSED</li>
 * </ol>
 */
public enum RunStatus {

    /** Every compared case passed, nothing broke, no call failed. */
    PASSED,

    /** Something moved or a call failed, but nothing is known to be broken. Drift and blips warn; they don't block. */
    WARNING,

    /** A case broke a property rule, or the provider failed on every case. */
    FAILED,

    /** Zero cases were compared against a baseline, so there is no result to report. */
    INCONCLUSIVE
}
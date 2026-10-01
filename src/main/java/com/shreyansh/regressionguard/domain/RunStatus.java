package com.shreyansh.regressionguard.domain;

/**
 * The overall outcome of one run.
 *
 * <p>The order between FAILED and INCONCLUSIVE is still an open design decision
 * (see README, "Open decisions"). The rules that pick a status arrive in slice 6.
 */
public enum RunStatus {

    /** Every compared case passed and nothing failed. */
    PASSED,

    /** At least one case DRIFTED and none failed. Drift warns; it does not block. */
    WARNING,

    /** At least one case is BROKEN or ERROR. */
    FAILED,

    /** Zero cases were actually compared against a baseline, so there is no result to report. */
    INCONCLUSIVE
}

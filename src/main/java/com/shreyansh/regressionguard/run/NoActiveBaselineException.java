package com.shreyansh.regressionguard.run;

/**
 * A run was requested before any baseline set was activated.
 * That's a setup step missing, not a result, so the API answers 409 Conflict.
 */
public class NoActiveBaselineException extends RuntimeException {

    public NoActiveBaselineException() {
        super("no baseline set is active; capture one with POST /baselines, then POST /baselines/{id}/activate");
    }
}
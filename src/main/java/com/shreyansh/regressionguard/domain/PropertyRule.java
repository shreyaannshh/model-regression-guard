package com.shreyansh.regressionguard.domain;

/**
 * One deterministic check on a response: valid JSON, required fields, a word limit.
 *
 * <p>The concrete rules arrive in slice 2. At that point this interface becomes
 * {@code sealed}, so the compiler knows every rule type that exists.
 */
public interface PropertyRule {

    /** A stable, readable name, for example "MaxWords(200)". */
    String name();

    /** Checks one response. Never throws for bad input: a bad response is a failed result, not an exception. */
    RuleResult check(String response);
}

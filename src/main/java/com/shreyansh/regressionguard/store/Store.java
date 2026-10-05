package com.shreyansh.regressionguard.store;

import com.shreyansh.regressionguard.domain.BaselineSet;
import com.shreyansh.regressionguard.domain.GoldenCase;
import java.util.List;
import java.util.Optional;

/**
 * Where golden cases, baseline sets and runs live.
 * In-memory for Phase 1; a Postgres implementation replaces it in Phase 2 without touching callers.
 * Run methods arrive in slice 6.
 */
public interface Store {

    // ---- golden cases ----

    /** Saves a new case. Throws DuplicateIdException if a case with the same id already exists. */
    void saveCase(GoldenCase goldenCase);

    Optional<GoldenCase> findCase(String id);

    /** All cases, oldest first. */
    List<GoldenCase> allCases();

    // ---- baseline sets ----

    /** Saves a new set. Sets are never updated, so a second save with the same id is a DuplicateIdException. */
    void saveBaselineSet(BaselineSet baselineSet);

    Optional<BaselineSet> findBaselineSet(String id);

    /** All sets, oldest first. */
    List<BaselineSet> allBaselineSets();

    /** Makes a set the reference for future runs. Throws NotFoundException for an unknown id. */
    void activateBaselineSet(String id);

    /** The set runs are compared against, or empty if none has been activated yet. */
    Optional<BaselineSet> activeBaselineSet();
}
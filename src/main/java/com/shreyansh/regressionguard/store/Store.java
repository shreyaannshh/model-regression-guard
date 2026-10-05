package com.shreyansh.regressionguard.store;

import com.shreyansh.regressionguard.domain.GoldenCase;
import java.util.List;
import java.util.Optional;

/**
 * Where golden cases, baseline sets and runs live.
 * In-memory for Phase 1; a Postgres implementation replaces it in Phase 2 without touching callers.
 * Baseline and run methods arrive in slices 5 and 6.
 */
public interface Store {

    /** Saves a new case. Throws DuplicateIdException if a case with the same id already exists. */
     void saveCase(GoldenCase goldenCase);

    Optional<GoldenCase> findCase(String id);

    /** All cases, oldest first. */
    List<GoldenCase> allCases();
}

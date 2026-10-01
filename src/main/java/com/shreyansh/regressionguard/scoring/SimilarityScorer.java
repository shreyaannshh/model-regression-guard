package com.shreyansh.regressionguard.scoring;

/**
 * Scores how similar two responses are, on a scale from 0.0 (nothing shared) to 1.0 (the same).
 *
 * <p>The threshold lives here, not in the verdict code, because each scorer's numbers mean
 * different things: 0.6 word overlap and 0.6 embedding cosine are not the same claim.
 * Swapping the scorer must swap its threshold with it.
 */
public interface SimilarityScorer {

    /** Similarity between 0.0 and 1.0. Symmetric: score(a, b) == score(b, a). */
    double score(String a, String b);

    /** Scores strictly below this are DRIFTED. */
    double driftThreshold();

    default boolean isDrifted(double score) {
        return score < driftThreshold();
    }
}

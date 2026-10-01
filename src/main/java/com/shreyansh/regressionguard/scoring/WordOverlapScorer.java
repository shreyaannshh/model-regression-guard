package com.shreyansh.regressionguard.scoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Phase 1 scorer: word-set overlap, also called Jaccard similarity.
 *
 * <p>Score = (words in both responses) / (distinct words across both responses).
 * Word order and repetition are ignored on purpose. It is cheap, needs no API,
 * and runs offline. Phase 2 replaces it with an embedding scorer behind the same interface.
 */
public final class WordOverlapScorer implements SimilarityScorer {

    /** Anything that is not a letter or digit, in any language, separates words. */
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final double driftThreshold;

    public WordOverlapScorer(double driftThreshold) {
        if (driftThreshold < 0.0 || driftThreshold > 1.0) {
            throw new IllegalArgumentException("driftThreshold must be between 0.0 and 1.0, was " + driftThreshold);
        }
        this.driftThreshold = driftThreshold;
    }

    @Override
    public double score(String a, String b) {
        Set<String> wordsA = words(Objects.requireNonNull(a, "a"));
        Set<String> wordsB = words(Objects.requireNonNull(b, "b"));

        // Two empty responses are identical. Without this check, 0 / 0 returns NaN.
        if (wordsA.isEmpty() && wordsB.isEmpty()) {
            return 1.0;
        }

        // Loop over the smaller set and look each word up in the larger one.
        Set<String> smaller = wordsA.size() <= wordsB.size() ? wordsA : wordsB;
        Set<String> larger = smaller == wordsA ? wordsB : wordsA;

        int shared = 0;
        for (String word : smaller) {
            if (larger.contains(word)) {
                shared++;
            }
        }

        // Distinct words across both = size of A + size of B - the words counted twice.
        int distinct = wordsA.size() + wordsB.size() - shared;
        return (double) shared / distinct;
    }

    @Override
    public double driftThreshold() {
        return driftThreshold;
    }

    /**
     * Lowercase, split on non-letters, drop empty pieces.
     * Locale.ROOT keeps lowercasing the same on every machine; under a Turkish
     * default locale, "TITLE".toLowerCase() produces a dotless i and stops matching "title".
     */
    static Set<String> words(String text) {
        return Arrays.stream(NON_WORD.split(text.toLowerCase(Locale.ROOT)))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }
}

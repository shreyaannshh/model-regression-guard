package com.shreyansh.regressionguard.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class WordOverlapScorerTest {

    private static final double EPS = 1e-9;

    private final WordOverlapScorer scorer = new WordOverlapScorer(0.6);

    @Test
    void identicalTextsScoreOne() {
        assertEquals(1.0, scorer.score("The capital of France is Paris", "The capital of France is Paris"), EPS);
    }

    @Test
    void textsWithNoSharedWordsScoreZero() {
        assertEquals(0.0, scorer.score("alpha beta", "gamma delta"), EPS);
    }

    @Test
    void partialOverlapIsSharedOverDistinct() {
        // shared {the, cat} = 2; distinct {the, cat, sat, ran} = 4
        assertEquals(0.5, scorer.score("the cat sat", "the cat ran"), EPS);
    }

    @Test
    void caseAndPunctuationAreIgnored() {
        assertEquals(1.0, scorer.score("Paris.", "paris"), EPS);
        assertEquals(1.0, scorer.score("Hello, WORLD!", "hello world"), EPS);
    }

    @Test
    void repeatedWordsCountOnce() {
        assertEquals(1.0, scorer.score("yes yes yes", "yes"), EPS);
    }

    @Test
    void wordOrderIsIgnored() {
        assertEquals(1.0, scorer.score("dog bites man", "man bites dog"), EPS);
    }

    @Test
    void bothEmptyScoreOneNotNaN() {
        assertEquals(1.0, scorer.score("", ""), EPS);
    }

    @Test
    void punctuationOnlyCountsAsEmpty() {
        assertEquals(1.0, scorer.score("  ...!!  ", ""), EPS);
    }

    @Test
    void oneEmptyScoresZero() {
        assertEquals(0.0, scorer.score("something", ""), EPS);
        assertEquals(0.0, scorer.score("", "something"), EPS);
    }

    @Test
    void scoreIsSymmetric() {
        String a = "the quick brown fox";
        String b = "the slow brown dog jumps";
        assertEquals(scorer.score(a, b), scorer.score(b, a), EPS);
    }

    @Test
    void nonEnglishLettersAreKeptAsWords() {
        assertEquals(1.0, scorer.score("Café naïve", "café NAÏVE"), EPS);
    }

    @Test
    void lowercasingDoesNotDependOnTheMachineLocale() {
        Locale original = Locale.getDefault();
        try {
            // Turkish lowercases "I" to a dotless "ı", which would break "TITLE" vs "title".
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(1.0, scorer.score("TITLE", "title"), EPS);
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void nullInputIsRejected() {
        assertThrows(NullPointerException.class, () -> scorer.score(null, "x"));
        assertThrows(NullPointerException.class, () -> scorer.score("x", null));
    }

    @Test
    void driftedMeansStrictlyBelowThreshold() {
        assertTrue(scorer.isDrifted(0.59));
        assertFalse(scorer.isDrifted(0.6));
        assertFalse(scorer.isDrifted(1.0));
    }

    @Test
    void thresholdOutsideZeroToOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WordOverlapScorer(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new WordOverlapScorer(1.5));
    }
}

package com.shreyansh.regressionguard.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shreyansh.regressionguard.domain.Baseline;
import com.shreyansh.regressionguard.domain.BaselineSet;
import com.shreyansh.regressionguard.domain.CaseResult;
import com.shreyansh.regressionguard.domain.GoldenCase;
import com.shreyansh.regressionguard.domain.MaxWords;
import com.shreyansh.regressionguard.domain.PropertyRule;
import com.shreyansh.regressionguard.domain.Run;
import com.shreyansh.regressionguard.domain.RunStatus;
import com.shreyansh.regressionguard.domain.Verdict;
import com.shreyansh.regressionguard.provider.FakeProviderClient;
import com.shreyansh.regressionguard.scoring.WordOverlapScorer;
import com.shreyansh.regressionguard.store.InMemoryStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");
    private static final double EPS = 1e-9;

    private final InMemoryStore store = new InMemoryStore();
    private final FakeProviderClient provider = new FakeProviderClient();
    private final RunService service = new RunService(
            store, provider, new WordOverlapScorer(0.6), Clock.fixed(NOW, ZoneOffset.UTC));

    /** Registers a case. Each one is a second apart so their order is predictable. */
    private void addCase(String id, String prompt, PropertyRule... rules) {
        Instant createdAt = NOW.plusSeconds(store.allCases().size());
        store.saveCase(new GoldenCase(id, prompt, List.of(rules), createdAt));
    }

    /** Stores and activates a baseline set holding these known-good answers, keyed by case id. */
    private void activateBaselines(Map<String, String> answers) {
        Map<String, Baseline> baselines = new HashMap<>();
        answers.forEach((caseId, text) ->
                baselines.put(caseId, new Baseline(caseId, text, NOW, FakeProviderClient.MODEL_REPORTED)));
        store.saveBaselineSet(new BaselineSet("set-1", NOW, FakeProviderClient.MODEL_REQUESTED, baselines, List.of()));
        store.activateBaselineSet("set-1");
    }

    @Test
    void runWithoutAnActiveBaselineIsRejected() {
        assertThrows(NoActiveBaselineException.class, service::run);
    }

    @Test
    void unchangedAnswerPasses() {
        addCase("capital", "Capital of France?");
        activateBaselines(Map.of("capital", "Paris is the capital of France."));
        provider.answer("Capital of France?", "Paris is the capital of France.");

        Run run = service.run();
        CaseResult result = run.caseResults().get(0);

        assertEquals(Verdict.PASS, result.verdict());
        assertEquals(1.0, result.similarity(), EPS);
        assertEquals(RunStatus.PASSED, run.status());
        assertEquals(1.0, run.driftScore(), EPS);
        assertEquals(1, run.comparedCount());
    }

    @Test
    void changedAnswerDriftsAndWarns() {
        addCase("capital", "Capital of France?");
        activateBaselines(Map.of("capital", "Paris is the capital of France."));
        provider.answer("Capital of France?", "I cannot help with that request.");

        Run run = service.run();

        assertEquals(Verdict.DRIFTED, run.caseResults().get(0).verdict());
        assertEquals(RunStatus.WARNING, run.status());
    }

    @Test
    void brokenRuleBeatsDriftAndSimilarityIsStillRecorded() {
        addCase("short", "Answer in three words", new MaxWords(3));
        activateBaselines(Map.of("short", "Paris"));
        provider.answer("Answer in three words", "I cannot help with that request.");

        Run run = service.run();
        CaseResult result = run.caseResults().get(0);

        assertEquals(Verdict.BROKEN, result.verdict());
        assertNotNull(result.similarity());
        assertFalse(result.ruleResults().get(0).passed());
        assertEquals(RunStatus.FAILED, run.status());
        assertNull(run.driftScore());
    }

    @Test
    void oneProviderErrorWarnsAndKeepsTheReason() {
        addCase("capital", "Capital of France?");
        addCase("greeting", "Say hello");
        activateBaselines(Map.of("capital", "Paris", "greeting", "Hello"));
        provider.answer("Capital of France?", "Paris").fail("Say hello", "provider returned HTTP 429");

        Run run = service.run();
        CaseResult failed = run.caseResults().get(1);

        assertEquals(Verdict.ERROR, failed.verdict());
        assertTrue(failed.error().contains("429"));
        assertNull(failed.response());
        assertEquals(RunStatus.WARNING, run.status());
        assertEquals(1.0, run.driftScore(), EPS);
    }

    @Test
    void providerFailingOnEveryCaseFails() {
        addCase("capital", "Capital of France?");
        addCase("greeting", "Say hello");
        activateBaselines(Map.of("capital", "Paris", "greeting", "Hello"));
        provider.fail("Capital of France?", "timeout").fail("Say hello", "timeout");

        Run run = service.run();

        assertEquals(RunStatus.FAILED, run.status());
        assertEquals(0, run.comparedCount());
    }

    @Test
    void caseWithoutBaselineIsNoBaselineAndRunIsInconclusive() {
        addCase("new-case", "Say hello");
        activateBaselines(Map.of("other-case", "anything"));
        provider.answer("Say hello", "Hello");

        Run run = service.run();

        assertEquals(Verdict.NO_BASELINE, run.caseResults().get(0).verdict());
        assertEquals(RunStatus.INCONCLUSIVE, run.status());
        assertNull(run.driftScore());
    }

    @Test
    void driftScoreAveragesOnlyComparedCases() {
        addCase("same", "Q1");
        addCase("moved", "Q2");
        addCase("short", "Q3", new MaxWords(1));
        activateBaselines(Map.of("same", "Paris", "moved", "Paris", "short", "yes"));
        provider.answer("Q1", "Paris").answer("Q2", "Berlin").answer("Q3", "two words");

        Run run = service.run();

        // PASS at 1.0 and DRIFTED at 0.0 count; the BROKEN case does not.
        assertEquals(0.5, run.driftScore(), EPS);
        assertEquals(2, run.comparedCount());
    }

    @Test
    void runWithNoCasesIsInconclusiveNotFailed() {
        activateBaselines(Map.of("x", "y"));

        Run run = service.run();

        assertTrue(run.caseResults().isEmpty());
        assertEquals(RunStatus.INCONCLUSIVE, run.status());
    }

    @Test
    void runIsSavedAndPointsAtItsBaselineSet() {
        addCase("capital", "Capital of France?");
        activateBaselines(Map.of("capital", "Paris"));
        provider.answer("Capital of France?", "Paris");

        Run run = service.run();

        assertEquals("set-1", run.baselineSetId());
        assertEquals(FakeProviderClient.MODEL_REQUESTED, run.modelRequested());
        assertTrue(store.findRun(run.id()).isPresent());
    }
}
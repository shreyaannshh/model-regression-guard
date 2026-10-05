package com.shreyansh.regressionguard.baseline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shreyansh.regressionguard.domain.BaselineSet;
import com.shreyansh.regressionguard.domain.GoldenCase;
import com.shreyansh.regressionguard.domain.MaxWords;
import com.shreyansh.regressionguard.domain.PropertyRule;
import com.shreyansh.regressionguard.domain.SkippedCase;
import com.shreyansh.regressionguard.provider.FakeProviderClient;
import com.shreyansh.regressionguard.store.InMemoryStore;
import com.shreyansh.regressionguard.store.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BaselineServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-06T10:00:00Z");

    private final InMemoryStore store = new InMemoryStore();
    private final FakeProviderClient provider = new FakeProviderClient();
    private final BaselineService service = new BaselineService(store, provider, Clock.fixed(NOW, ZoneOffset.UTC));

    /** Registers a case. Each one is a second apart so their order is predictable. */
    private void addCase(String id, String prompt, PropertyRule... rules) {
        Instant createdAt = NOW.plusSeconds(store.allCases().size());
        store.saveCase(new GoldenCase(id, prompt, List.of(rules), createdAt));
    }

    @Test
    void capturesEveryCaseAndRecordsBothModelNames() {
        addCase("capital", "Capital of France?");
        addCase("greeting", "Say hello");
        provider.answer("Capital of France?", "Paris").answer("Say hello", "Hello!");

        BaselineSet set = service.capture();

        assertEquals(Set.of("capital", "greeting"), set.baselines().keySet());
        assertEquals("Paris", set.baselineFor("capital").orElseThrow().response());
        assertEquals(FakeProviderClient.MODEL_REQUESTED, set.modelRequested());
        assertEquals(FakeProviderClient.MODEL_REPORTED, set.baselineFor("capital").orElseThrow().modelReported());
        assertTrue(set.skipped().isEmpty());
        assertEquals(NOW, set.createdAt());
    }

    @Test
    void newSetIsStoredButNotActive() {
        addCase("capital", "Capital of France?");
        provider.answer("Capital of France?", "Paris");

        BaselineSet set = service.capture();

        assertTrue(store.findBaselineSet(set.id()).isPresent());
        assertTrue(store.activeBaselineSet().isEmpty());
    }

    @Test
    void providerFailureSkipsThatCaseWithTheReason() {
        addCase("capital", "Capital of France?");
        addCase("greeting", "Say hello");
        provider.answer("Capital of France?", "Paris").fail("Say hello", "provider returned HTTP 429");

        BaselineSet set = service.capture();

        assertEquals(Set.of("capital"), set.baselines().keySet());
        assertEquals(List.of(new SkippedCase("greeting", "provider returned HTTP 429")), set.skipped());
    }

    @Test
    void answerThatFailsItsOwnRulesIsSkipped() {
        addCase("short", "Answer in one word", new MaxWords(1));
        addCase("capital", "Capital of France?");
        provider.answer("Answer in one word", "two words").answer("Capital of France?", "Paris");

        BaselineSet set = service.capture();

        assertEquals(Set.of("capital"), set.baselines().keySet());
        assertTrue(set.skipped().get(0).reason().contains("MaxWords(1)"), set.skipped().toString());
    }

    @Test
    void noCasesRegisteredIsRejected() {
        assertThrows(NothingCapturedException.class, service::capture);
        assertTrue(store.allBaselineSets().isEmpty());
    }

    @Test
    void everyCaseSkippedIsRejectedAndNothingIsStored() {
        addCase("capital", "Capital of France?");
        addCase("greeting", "Say hello");
        provider.fail("Capital of France?", "HTTP 404 model_not_found").fail("Say hello", "HTTP 404 model_not_found");

        NothingCapturedException e = assertThrows(NothingCapturedException.class, service::capture);

        assertEquals(2, e.skipped().size());
        assertTrue(store.allBaselineSets().isEmpty());
    }

    @Test
    void capturingAgainCreatesANewSetAndLeavesTheOldOneUnchanged() {
        addCase("capital", "Capital of France?");
        provider.answer("Capital of France?", "Paris");
        BaselineSet first = service.capture();

        provider.answer("Capital of France?", "Paris is the capital.");
        BaselineSet second = service.capture();

        assertNotEquals(first.id(), second.id());
        String stillFirst = store.findBaselineSet(first.id()).orElseThrow()
                .baselineFor("capital").orElseThrow().response();
        assertEquals("Paris", stillFirst);
    }

    @Test
    void activateSwitchesTheActiveSet() {
        addCase("capital", "Capital of France?");
        provider.answer("Capital of France?", "Paris");
        BaselineSet first = service.capture();
        BaselineSet second = service.capture();

        service.activate(first.id());
        assertEquals(first.id(), store.activeBaselineSet().orElseThrow().id());

        service.activate(second.id());
        assertEquals(second.id(), store.activeBaselineSet().orElseThrow().id());
    }

    @Test
    void activatingAnUnknownIdKeepsTheCurrentSet() {
        addCase("capital", "Capital of France?");
        provider.answer("Capital of France?", "Paris");
        BaselineSet set = service.capture();
        service.activate(set.id());

        assertThrows(NotFoundException.class, () -> service.activate("does-not-exist"));
        assertEquals(set.id(), store.activeBaselineSet().orElseThrow().id());
    }
}
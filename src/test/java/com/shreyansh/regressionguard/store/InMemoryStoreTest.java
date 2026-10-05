package com.shreyansh.regressionguard.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shreyansh.regressionguard.domain.BaselineSet;
import com.shreyansh.regressionguard.domain.GoldenCase;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryStoreTest {

    private final InMemoryStore store = new InMemoryStore();

    private static GoldenCase golden(String id, String prompt, Instant createdAt) {
        return new GoldenCase(id, prompt, List.of(), createdAt);
    }

    private static BaselineSet emptySet(String id) {
        return new BaselineSet(id, Instant.parse("2026-10-06T10:00:00Z"), "model", null, null);
    }

    @Test
    void savedCaseCanBeFound() {
        store.saveCase(golden("a", "prompt a", Instant.parse("2026-10-01T10:00:00Z")));
        assertEquals("prompt a", store.findCase("a").orElseThrow().prompt());
    }

    @Test
    void unknownIdIsEmpty() {
        assertTrue(store.findCase("missing").isEmpty());
    }

    @Test
    void duplicateIdIsRejectedAndOriginalIsKept() {
        store.saveCase(golden("a", "original", Instant.parse("2026-10-01T10:00:00Z")));
        assertThrows(DuplicateIdException.class,
                () -> store.saveCase(golden("a", "replacement", Instant.parse("2026-10-01T11:00:00Z"))));
        assertEquals("original", store.findCase("a").orElseThrow().prompt());
    }

    @Test
    void allCasesAreOldestFirst() {
        store.saveCase(golden("b", "prompt b", Instant.parse("2026-10-01T12:00:00Z")));
        store.saveCase(golden("a", "prompt a", Instant.parse("2026-10-01T10:00:00Z")));
        List<String> ids = store.allCases().stream().map(GoldenCase::id).toList();
        assertEquals(List.of("a", "b"), ids);
    }

    @Test
    void noSetIsActiveAtFirst() {
        assertTrue(store.activeBaselineSet().isEmpty());
    }

    @Test
    void baselineSetsAreNeverOverwritten() {
        store.saveBaselineSet(emptySet("set-1"));
        assertThrows(DuplicateIdException.class, () -> store.saveBaselineSet(emptySet("set-1")));
    }

    @Test
    void activatingAnUnknownSetIsNotFound() {
        assertThrows(NotFoundException.class, () -> store.activateBaselineSet("missing"));
        assertTrue(store.activeBaselineSet().isEmpty());
    }
}
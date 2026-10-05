package com.shreyansh.regressionguard.store;

import com.shreyansh.regressionguard.domain.GoldenCase;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryStore implements Store {
    private final Map<String, GoldenCase> cases = new ConcurrentHashMap<>();

    @Override
    public void saveCase(GoldenCase goldenCase) {
        GoldenCase existing = cases.putIfAbsent(goldenCase.id(), goldenCase);
        if (existing != null) {
            throw new DuplicateIdException("GoldenCase", goldenCase.id());
        }
    }

    @Override
    public Optional<GoldenCase> findCase(String id) {
        return Optional.ofNullable(cases.get(id));
    }

    @Override
    public List<GoldenCase> allCases() {
        return cases.values().stream().sorted(Comparator.comparing(GoldenCase::createdAt).thenComparing(GoldenCase::id))
                .toList();
    }
}
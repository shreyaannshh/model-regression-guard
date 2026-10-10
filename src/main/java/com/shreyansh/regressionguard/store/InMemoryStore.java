package com.shreyansh.regressionguard.store;

import com.shreyansh.regressionguard.domain.Run;
import com.shreyansh.regressionguard.domain.BaselineSet;
import com.shreyansh.regressionguard.domain.GoldenCase;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryStore implements Store {

    private final Map<String, GoldenCase> cases = new ConcurrentHashMap<>();
    private final Map<String, BaselineSet> baselineSets = new ConcurrentHashMap<>();
    private final Map<String, Run> runs = new ConcurrentHashMap<>();

    // One pointer, not an "active" flag on each set: exactly one active set is guaranteed by the data's shape.
    // volatile so a request thread always sees the latest activation.
    private volatile String activeBaselineSetId;

    // ---- golden cases ----

    @Override
    public void saveCase(GoldenCase goldenCase) {
        // putIfAbsent checks and inserts in one atomic step,
        // so two requests with the same id at the same moment can't both succeed.
        GoldenCase existing = cases.putIfAbsent(goldenCase.id(), goldenCase);
        if (existing != null) {
            throw new DuplicateIdException("case", goldenCase.id());
        }
    }

    @Override
    public Optional<GoldenCase> findCase(String id) {
        return Optional.ofNullable(cases.get(id));
    }

    @Override
    public List<GoldenCase> allCases() {
        return cases.values().stream()
                .sorted(Comparator.comparing(GoldenCase::createdAt).thenComparing(GoldenCase::id))
                .toList();
    }

    // ---- baseline sets ----

    @Override
    public void saveBaselineSet(BaselineSet baselineSet) {
        BaselineSet existing = baselineSets.putIfAbsent(baselineSet.id(), baselineSet);
        if (existing != null) {
            throw new DuplicateIdException("baseline set", baselineSet.id());
        }
    }

    @Override
    public Optional<BaselineSet> findBaselineSet(String id) {
        return Optional.ofNullable(baselineSets.get(id));
    }

    @Override
    public List<BaselineSet> allBaselineSets() {
        return baselineSets.values().stream()
                .sorted(Comparator.comparing(BaselineSet::createdAt).thenComparing(BaselineSet::id))
                .toList();
    }

    @Override
    public void activateBaselineSet(String id) {
        // Sets are never deleted, so once this check passes the id stays valid.
        if (!baselineSets.containsKey(id)) {
            throw new NotFoundException("baseline set", id);
        }
        activeBaselineSetId = id;
    }

    @Override
    public Optional<BaselineSet> activeBaselineSet() {
        String id = activeBaselineSetId;
        return id == null ? Optional.empty() : Optional.ofNullable(baselineSets.get(id));
    }

        // ---- runs ----

    @Override
    public void saveRun(Run run) {
        Run existing = runs.putIfAbsent(run.id(), run);
        if (existing != null) {
            throw new DuplicateIdException("run", run.id());
        }
    }

    @Override
    public Optional<Run> findRun(String id) {
        return Optional.ofNullable(runs.get(id));
    }

    @Override
    public List<Run> allRuns() {
        return runs.values().stream()
                .sorted(Comparator.comparing(Run::startedAt).thenComparing(Run::id))
                .toList();
    }
}
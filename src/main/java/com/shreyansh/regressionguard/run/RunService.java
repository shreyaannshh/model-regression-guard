package com.shreyansh.regressionguard.run;

import com.shreyansh.regressionguard.domain.Baseline;
import com.shreyansh.regressionguard.domain.BaselineSet;
import com.shreyansh.regressionguard.domain.CaseResult;
import com.shreyansh.regressionguard.domain.GoldenCase;
import com.shreyansh.regressionguard.domain.PropertyRule;
import com.shreyansh.regressionguard.domain.RuleResult;
import com.shreyansh.regressionguard.domain.Run;
import com.shreyansh.regressionguard.domain.RunStatus;
import com.shreyansh.regressionguard.domain.Verdict;
import com.shreyansh.regressionguard.provider.ProviderClient;
import com.shreyansh.regressionguard.provider.ProviderException;
import com.shreyansh.regressionguard.provider.ProviderResponse;
import com.shreyansh.regressionguard.scoring.SimilarityScorer;
import com.shreyansh.regressionguard.store.Store;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Replays every golden case against the active baseline set and judges each one. */
@Service
public class RunService {

    private final Store store;
    private final ProviderClient provider;
    private final SimilarityScorer scorer;
    private final Clock clock;

    public RunService(Store store, ProviderClient provider, SimilarityScorer scorer, Clock clock) {
        this.store = store;
        this.provider = provider;
        this.scorer = scorer;
        this.clock = clock;
    }

    /** Replays every case, saves the run, and returns it. Throws NoActiveBaselineException if nothing is active. */
    public Run run() {
        BaselineSet baselineSet = store.activeBaselineSet().orElseThrow(NoActiveBaselineException::new);
        Instant startedAt = clock.instant();

        List<CaseResult> results = new ArrayList<>();
        for (GoldenCase goldenCase : store.allCases()) {
            results.add(evaluate(goldenCase, baselineSet));
        }

        Run run = new Run(
                UUID.randomUUID().toString(),
                startedAt,
                clock.instant(),
                baselineSet.id(),
                provider.modelRequested(),
                results,
                driftScore(results),
                comparedCount(results),
                statusFor(results));
        store.saveRun(run);
        return run;
    }

    /** One case: call, check rules, score against the baseline, pick the verdict. */
    CaseResult evaluate(GoldenCase goldenCase, BaselineSet baselineSet) {
        ProviderResponse response;
        try {
            response = provider.complete(goldenCase.prompt());
        } catch (ProviderException e) {
            // A fact about the network, not the model: stop here, before any rule runs.
            return new CaseResult(goldenCase.id(), null, null, List.of(), null, Verdict.ERROR, e.getMessage());
        }

        List<RuleResult> ruleResults = checkRules(goldenCase.propertyRules(), response.text());

        // Similarity is computed whenever a baseline exists, even for BROKEN cases, so it is always on record.
        Optional<Baseline> baseline = baselineSet.baselineFor(goldenCase.id());
        Double similarity = baseline.isPresent()
                ? scorer.score(baseline.get().response(), response.text())
                : null;

        Verdict verdict = verdictFor(ruleResults, similarity);
        return new CaseResult(goldenCase.id(), response.text(), response.modelReported(),
                ruleResults, similarity, verdict, null);
    }

    /** Precedence: BROKEN, then NO_BASELINE, then DRIFTED, then PASS. ERROR is decided before this is reached. */
    Verdict verdictFor(List<RuleResult> ruleResults, Double similarity) {
        boolean anyRuleFailed = ruleResults.stream().anyMatch(result -> !result.passed());
        if (anyRuleFailed) {
            return Verdict.BROKEN;
        }
        if (similarity == null) {
            return Verdict.NO_BASELINE;
        }
        if (scorer.isDrifted(similarity)) {
            return Verdict.DRIFTED;
        }
        return Verdict.PASS;
    }

    /** Run status, first match wins. See RunStatus for the rules. */
    static RunStatus statusFor(List<CaseResult> results) {
        if (countOf(results, Verdict.BROKEN) > 0) {
            return RunStatus.FAILED;
        }
        long errors = countOf(results, Verdict.ERROR);
        // The isEmpty check matters: with zero cases, "every case errored" would be vacuously true.
        if (!results.isEmpty() && errors == results.size()) {
            return RunStatus.FAILED;
        }
        if (comparedCount(results) == 0) {
            return RunStatus.INCONCLUSIVE;
        }
        if (countOf(results, Verdict.DRIFTED) > 0 || errors > 0) {
            return RunStatus.WARNING;
        }
        return RunStatus.PASSED;
    }

    /** Mean similarity over compared cases (PASS or DRIFTED). Null, not 0, when none were compared. */
    static Double driftScore(List<CaseResult> results) {
        double sum = 0;
        int count = 0;
        for (CaseResult result : results) {
            if (result.verdict().isCompared()) {
                sum += result.similarity();
                count++;
            }
        }
        return count == 0 ? null : sum / count;
    }

    static int comparedCount(List<CaseResult> results) {
        int count = 0;
        for (CaseResult result : results) {
            if (result.verdict().isCompared()) {
                count++;
            }
        }
        return count;
    }

    private static long countOf(List<CaseResult> results, Verdict verdict) {
        return results.stream().filter(result -> result.verdict() == verdict).count();
    }

    private static List<RuleResult> checkRules(List<PropertyRule> rules, String text) {
        List<RuleResult> results = new ArrayList<>();
        for (PropertyRule rule : rules) {
            results.add(rule.check(text));
        }
        return results;
    }
}
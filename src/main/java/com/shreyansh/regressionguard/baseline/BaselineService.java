package com.shreyansh.regressionguard.baseline;

import com.shreyansh.regressionguard.domain.Baseline;
import com.shreyansh.regressionguard.domain.BaselineSet;
import com.shreyansh.regressionguard.domain.GoldenCase;
import com.shreyansh.regressionguard.domain.PropertyRule;
import com.shreyansh.regressionguard.domain.RuleResult;
import com.shreyansh.regressionguard.domain.SkippedCase;
import com.shreyansh.regressionguard.provider.ProviderClient;
import com.shreyansh.regressionguard.provider.ProviderException;
import com.shreyansh.regressionguard.provider.ProviderResponse;
import com.shreyansh.regressionguard.store.NotFoundException;
import com.shreyansh.regressionguard.store.Store;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Captures known-good answers and decides which set future runs are compared against.
 *
 * <p>Capture is partial success with per-case reporting: every case that can be captured is,
 * and every case that can't is listed with its reason. That's safe because a new set is never
 * trusted until someone activates it.
 */
@Service
public class BaselineService {

    private final Store store;
    private final ProviderClient provider;
    private final Clock clock;

    public BaselineService(Store store, ProviderClient provider, Clock clock) {
        this.store = store;
        this.provider = provider;
        this.clock = clock;
    }

    /** Sends every golden case to the provider and stores the answers as a new, inactive baseline set. */
    public BaselineSet capture() {
        List<GoldenCase> cases = store.allCases();
        if (cases.isEmpty()) {
            throw new NothingCapturedException("no golden cases are registered; add some with POST /cases", List.of());
        }

        Map<String, Baseline> baselines = new HashMap<>();
        List<SkippedCase> skipped = new ArrayList<>();

        for (GoldenCase goldenCase : cases) {
            ProviderResponse response;
            try {
                response = provider.complete(goldenCase.prompt());
            } catch (ProviderException e) {
                skipped.add(new SkippedCase(goldenCase.id(), e.getMessage()));
                continue;
            }

            List<String> failures = failedRules(goldenCase.propertyRules(), response.text());
            if (!failures.isEmpty()) {
                // Never guard a known-bad answer.
                skipped.add(new SkippedCase(goldenCase.id(), "failed its own rules: " + String.join("; ", failures)));
                continue;
            }

            baselines.put(goldenCase.id(),
                    new Baseline(goldenCase.id(), response.text(), clock.instant(), response.modelReported()));
        }

        if (baselines.isEmpty()) {
            throw new NothingCapturedException("every case was skipped, so no baseline set was stored", skipped);
        }

        BaselineSet baselineSet = new BaselineSet(
                UUID.randomUUID().toString(),
                clock.instant(),
                provider.modelRequested(),
                baselines,
                skipped);
        store.saveBaselineSet(baselineSet);
        return baselineSet;
    }

    /** Makes a set the reference for future runs. Throws NotFoundException for an unknown id. */
    public BaselineSet activate(String id) {
        store.activateBaselineSet(id);
        return store.findBaselineSet(id).orElseThrow(() -> new NotFoundException("baseline set", id));
    }

    /** "RuleName: reason" for every rule the response failed; empty when all passed. */
    private static List<String> failedRules(List<PropertyRule> rules, String text) {
        List<String> failures = new ArrayList<>();
        for (PropertyRule rule : rules) {
            RuleResult result = rule.check(text);
            if (!result.passed()) {
                failures.add(result.ruleName() + ": " + result.message());
            }
        }
        return failures;
    }
}
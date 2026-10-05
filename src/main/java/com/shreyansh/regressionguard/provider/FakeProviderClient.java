package com.shreyansh.regressionguard.provider;

import java.util.HashMap;
import java.util.Map;

/**
 * A scripted stand-in for a real LLM, for tests. Each prompt either gets a fixed answer or fails.
 * Answers can be changed between calls, which is how later tests simulate a model drifting.
 */
public class FakeProviderClient implements ProviderClient {

    public static final String MODEL_REQUESTED = "fake-model";
    public static final String MODEL_REPORTED = "fake-model-2026-10";

    private final Map<String, String> answers = new HashMap<>();
    private final Map<String, String> failures = new HashMap<>();

    /** From now on, this prompt gets this answer. */
    public FakeProviderClient answer(String prompt, String text) {
        failures.remove(prompt);
        answers.put(prompt, text);
        return this;
    }

    /** From now on, this prompt fails the way a timeout or HTTP error would. */
    public FakeProviderClient fail(String prompt, String reason) {
        answers.remove(prompt);
        failures.put(prompt, reason);
        return this;
    }

    @Override
    public ProviderResponse complete(String prompt) {
        if (failures.containsKey(prompt)) {
            throw new ProviderException(failures.get(prompt));
        }
        if (answers.containsKey(prompt)) {
            return new ProviderResponse(answers.get(prompt), MODEL_REPORTED);
        }
        throw new IllegalStateException("test forgot to script an answer for: " + prompt);
    }

    @Override
    public String modelRequested() {
        return MODEL_REQUESTED;
    }
}
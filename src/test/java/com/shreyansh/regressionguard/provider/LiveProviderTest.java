package com.shreyansh.regressionguard.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Calls the real Groq API. Skipped unless PROVIDER_API_KEY is set,
 * so it never runs in CI or on a machine without a key.
 */
@EnabledIfEnvironmentVariable(named = "PROVIDER_API_KEY", matches = ".+")
class LiveProviderTest {

    @Test
    void realProviderAnswers() {
        String model = System.getenv().getOrDefault("PROVIDER_MODEL", "openai/gpt-oss-20b");
        var props = new ProviderProperties(
                "https://api.groq.com/openai/v1",
                System.getenv("PROVIDER_API_KEY"),
                model,
                Duration.ofSeconds(30));

        ProviderResponse response = new OpenAiCompatibleClient(props).complete("Reply with exactly one word: Paris");

        System.out.println("LIVE model=" + response.modelReported() + " text=" + response.text());
        assertFalse(response.text().isBlank());
    }
}
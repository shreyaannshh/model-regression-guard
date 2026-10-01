package com.shreyansh.regressionguard.scoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScoringConfig {

    /**
     * 0.6 is a placeholder, not a tuned value. Phase 2 replaces the single global
     * threshold with a per-case noise floor measured at capture time.
     */
    @Bean
    public SimilarityScorer similarityScorer(
            @Value("${scoring.word-overlap.drift-threshold:0.6}") double driftThreshold) {
        return new WordOverlapScorer(driftThreshold);
    }
}

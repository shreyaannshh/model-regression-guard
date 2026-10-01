package com.shreyansh.regressionguard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.shreyansh.regressionguard.scoring.SimilarityScorer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RegressionGuardApplicationTests {

    @Autowired
    private SimilarityScorer scorer;

    @Test
    void contextLoadsWithConfiguredThreshold() {
        assertEquals(0.6, scorer.driftThreshold(), 1e-9);
    }
}

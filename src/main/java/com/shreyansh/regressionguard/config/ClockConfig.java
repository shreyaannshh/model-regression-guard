package com.shreyansh.regressionguard.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** One clock for the whole app. Tests pass a fixed clock instead, so timestamps are predictable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
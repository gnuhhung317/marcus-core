package io.marcus.api.config;

import io.marcus.domain.service.EquityCurveMetricsCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enable scheduling support for the application. Required for the
 * {@code @Scheduled} annotations in {@code LeaderboardMetricsCalculator} and
 * other scheduled components.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    @Bean
    public EquityCurveMetricsCalculator equityCurveMetricsCalculator() {
        return new EquityCurveMetricsCalculator();
    }
}

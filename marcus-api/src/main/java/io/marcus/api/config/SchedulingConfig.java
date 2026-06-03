package io.marcus.api.config;

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
    // Enables @Scheduled annotations across the application
}

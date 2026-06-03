package io.marcus.domain.model;

import java.time.LocalDateTime;

public record BotBacktestRun(
        String runId,
        String botId,
        String runName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String metricsJson,
        LocalDateTime createdAt
) {
}

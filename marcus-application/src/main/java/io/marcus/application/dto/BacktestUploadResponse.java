package io.marcus.application.dto;

import java.time.LocalDateTime;

public record BacktestUploadResponse(
        String runId,
        String botId,
        String runName,
        int equityPoints,
        int closedTrades,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}

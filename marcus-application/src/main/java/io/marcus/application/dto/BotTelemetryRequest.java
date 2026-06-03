package io.marcus.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record BotTelemetryRequest(
        LocalDateTime timestamp,
        BigDecimal equity,
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl,
        Map<String, Object> metrics
) {
}

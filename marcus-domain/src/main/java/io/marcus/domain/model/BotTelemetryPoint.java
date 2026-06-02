package io.marcus.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BotTelemetryPoint(
        String botId,
        LocalDateTime timestamp,
        BigDecimal equity,
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl
) {
}

package io.marcus.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BotTelemetryRequest(
        @NotNull LocalDateTime timestamp,
        @NotNull BigDecimal equity,
        @NotNull BigDecimal realizedPnl,
        @NotNull BigDecimal unrealizedPnl
) {
}

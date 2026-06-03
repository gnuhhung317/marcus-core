package io.marcus.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

public record BacktestUploadRequest(
        String runName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Map<String, Object> metrics,
        @NotEmpty List<@Valid EquityPoint> equityHistory,
        @NotNull List<@Valid ClosedTrade> closedTrades
) {

    public record EquityPoint(
            @NotNull LocalDateTime timestamp,
            @NotNull BigDecimal cash,
            @NotNull BigDecimal equity,
            @NotNull BigDecimal realizedPnl,
            @NotNull BigDecimal unrealizedPnl,
            @NotNull BigDecimal totalFees
    ) {
    }

    public record ClosedTrade(
            @NotBlank String symbol,
            @NotBlank String marketType,
            @NotBlank String side,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal entryPrice,
            @NotNull BigDecimal exitPrice,
            @NotNull BigDecimal pnl,
            @NotNull BigDecimal fees,
            @NotNull LocalDateTime entryTimestamp,
            @NotNull LocalDateTime exitTimestamp,
            BigDecimal durationSeconds
    ) {
    }
}

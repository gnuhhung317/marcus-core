package io.marcus.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BotDryRunSyncRequest(
        @NotNull @Valid Portfolio portfolio,
        @NotNull List<@Valid Position> positions,
        @NotNull List<@Valid ClosedTrade> closedTrades
) {

    public record Portfolio(
            @NotNull LocalDateTime timestamp,
            @NotNull BigDecimal cash,
            @NotNull BigDecimal equity,
            @NotNull BigDecimal realizedPnl,
            @NotNull BigDecimal unrealizedPnl,
            @NotNull BigDecimal totalFees
    ) {
    }

    public record Position(
            @NotBlank String positionId,
            @NotBlank String symbol,
            @NotBlank String marketType,
            @NotBlank String side,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal entryPrice,
            @NotNull BigDecimal currentPrice,
            @NotNull BigDecimal unrealizedPnl,
            @NotNull LocalDateTime openedAt,
            String sourceSignalId
    ) {
    }

    public record ClosedTrade(
            @NotBlank String tradeId,
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
            String entrySignalId,
            String exitSignalId
    ) {
    }
}

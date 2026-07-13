package io.marcus.application.dto;

import io.marcus.domain.vo.MarginMode;
import io.marcus.domain.vo.MarketType;
import io.marcus.domain.vo.OrderType;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Inbound request DTO for capturing a trading signal.
 *
 * <p>The API validates required fields at the boundary and the application layer must
 * not backfill hidden defaults.
 */
public record CaptureSignalRequest(

        @NotBlank(message = "signalId is required")
        String signalId,

        @NotBlank(message = "botId is required")
        String botId,

        @NotBlank(message = "symbol is required")
        String symbol,

        @NotNull(message = "action is required")
        SignalAction action,

        @NotNull(message = "marketType is required")
        MarketType marketType,

        @NotNull(message = "orderType is required")
        OrderType orderType,

        BigDecimal entry,

        BigDecimal stopLoss,

        BigDecimal takeProfit,

        @Positive(message = "amount must be positive when provided")
        BigDecimal amount,

        @Min(value = 1, message = "leverage must be at least 1")
        @Max(value = 125, message = "leverage must not exceed 125")
        Integer leverage,

        MarginMode marginMode,

        Boolean reduceOnly,

        SignalStatus status,

        @NotNull(message = "generatedTimestamp is required")
        LocalDateTime generatedTimestamp,

        String timeframe,

        Map<String, Object> metadata,

        Map<String, Object> policies
) {

    @AssertTrue(message = "entry must be provided when orderType is LIMIT")
    public boolean isEntryProvidedForLimit() {
        if (orderType == null) {
            return true;
        }
        if (orderType == OrderType.LIMIT) {
            return entry != null && entry.signum() > 0;
        }
        return true;
    }
}

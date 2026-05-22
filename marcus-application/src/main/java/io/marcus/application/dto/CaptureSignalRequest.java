package io.marcus.application.dto;

import io.marcus.domain.vo.MarginMode;
import io.marcus.domain.vo.MarketType;
import io.marcus.domain.vo.OrderType;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
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
 * <p><b>Validation rules:</b>
 * <ul>
 *   <li>{@code entry} is only required for {@code LIMIT} orders — validated in
 *       {@code CaptureSignalUseCase}.</li>
 *   <li>{@code amount} is optional here; executor falls back to its configured default
 *       when absent.</li>
 *   <li>Futures-specific fields ({@code leverage}, {@code marginMode}) are ignored
 *       for {@code SPOT} / {@code MARGIN} market types.</li>
 * </ul>
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

        /**
         * Market type. Defaults to {@code SPOT} when absent.
         */
        MarketType marketType,

        /**
         * Order type. Defaults to {@code LIMIT} when absent.
         * When {@code LIMIT}, {@code entry} must be provided.
         */
        OrderType orderType,

        /**
         * Entry price. Required for {@code LIMIT} orders; optional for {@code MARKET}.
         * Validated conditionally in {@code CaptureSignalUseCase}.
         */
        BigDecimal entry,

        BigDecimal stopLoss,

        BigDecimal takeProfit,

        /**
         * Order size in base asset. When null, executor falls back to its
         * {@code DEFAULT_ORDER_AMOUNT} environment variable.
         */
        @Positive(message = "amount must be positive when provided")
        BigDecimal amount,

        /**
         * Futures leverage multiplier (1–125). Defaults to 1 when absent.
         * Ignored for non-FUTURE market types.
         */
        @Min(value = 1, message = "leverage must be at least 1")
        @Max(value = 125, message = "leverage must not exceed 125")
        Integer leverage,

        /**
         * Futures margin mode. Defaults to {@code CROSS} when absent.
         * Ignored for non-FUTURE market types.
         */
        MarginMode marginMode,

        /**
         * Explicit reduce-only override. When null, the executor derives it from action:
         * {@code CLOSE_LONG} / {@code CLOSE_SHORT} → true; OPEN variants → false.
         */
        Boolean reduceOnly,

        SignalStatus status,

        LocalDateTime generatedTimestamp,

        String timeframe,

        Map<String, Object> metadata
) {
}

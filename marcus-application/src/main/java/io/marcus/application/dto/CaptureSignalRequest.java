package io.marcus.application.dto;

import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record CaptureSignalRequest(
        @NotBlank(message = "signalId is required")
        String signalId,

        @NotBlank(message = "botId is required")
        String botId,

        @NotBlank(message = "symbol is required")
        String symbol,

        @NotNull(message = "action is required")
        SignalAction action,

        @NotNull(message = "entry is required")
        BigDecimal entry,

        BigDecimal stopLoss,

        BigDecimal takeProfit,

        SignalStatus status,

        LocalDateTime generatedTimestamp,

        String timeframe,

        Map<String, Object> metadata
) {
}

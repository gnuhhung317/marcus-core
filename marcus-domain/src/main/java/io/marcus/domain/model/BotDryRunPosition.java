package io.marcus.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BotDryRunPosition(
        String botId,
        String positionId,
        String symbol,
        String marketType,
        String side,
        BigDecimal quantity,
        BigDecimal entryPrice,
        BigDecimal currentPrice,
        BigDecimal unrealizedPnl,
        LocalDateTime openedAt,
        String sourceSignalId,
        String status
) {
}

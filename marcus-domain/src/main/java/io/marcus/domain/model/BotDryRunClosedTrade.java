package io.marcus.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BotDryRunClosedTrade(
        String botId,
        String tradeId,
        String symbol,
        String marketType,
        String side,
        BigDecimal quantity,
        BigDecimal entryPrice,
        BigDecimal exitPrice,
        BigDecimal pnl,
        BigDecimal fees,
        LocalDateTime entryTimestamp,
        LocalDateTime exitTimestamp,
        String entrySignalId,
        String exitSignalId
) {
}

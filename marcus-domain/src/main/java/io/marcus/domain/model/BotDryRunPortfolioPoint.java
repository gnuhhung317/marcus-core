package io.marcus.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BotDryRunPortfolioPoint(
        String botId,
        LocalDateTime timestamp,
        BigDecimal cash,
        BigDecimal equity,
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl,
        BigDecimal totalFees
) {
}

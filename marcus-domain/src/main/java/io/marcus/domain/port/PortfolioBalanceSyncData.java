package io.marcus.domain.port;

import java.math.BigDecimal;

public record PortfolioBalanceSyncData(
        BigDecimal total,
        BigDecimal available,
        BigDecimal used,
        BigDecimal unrealizedPnl,
        String exchangeId,
        String currency,
        String executionMode
) {
}

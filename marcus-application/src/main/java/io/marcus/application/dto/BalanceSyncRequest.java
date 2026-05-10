package io.marcus.application.dto;

import java.math.BigDecimal;

public record BalanceSyncRequest(
        BigDecimal total,
        BigDecimal available,
        BigDecimal used,
        BigDecimal unrealizedPnl,
        String exchange
) {}

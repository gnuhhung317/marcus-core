package io.marcus.application.dto;

import java.math.BigDecimal;

public record UpdateBotMetadataRequest(
        String name,
        String description,
        String tradingPair,
        String exchangeId,
        BigDecimal price,
        String riskLevel
) {
}

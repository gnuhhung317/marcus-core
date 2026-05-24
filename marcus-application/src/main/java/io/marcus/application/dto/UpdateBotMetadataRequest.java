package io.marcus.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdateBotMetadataRequest(
        String name,
        String description,
        String tradingPair,
        String exchangeId,
        BigDecimal price,
        String riskLevel,
        List<String> assetPairs
) {
}

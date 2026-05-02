package io.marcus.application.dto;

import lombok.Builder;

@Builder
public record BotSummaryResult(
        String botId,
        String botName,
        String description,
        String status,
        String tradingPair,
        String exchange,
        String apiKey
        ) {

}

package io.marcus.application.dto;


import lombok.Builder;


@Builder
public record BotRegistrationResult(
        String botId,
        String botName,
        String description,
        String name,
        String apiKey,
        String rawSecret,
        String status,
        String tradingPair,
        String exchange
) {
}
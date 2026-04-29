package io.marcus.application.dto;

public record BotSubscriptionResult(
        String botId,
        String wsToken,
        String status
        ) {

}

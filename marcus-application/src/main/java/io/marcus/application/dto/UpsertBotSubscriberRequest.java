package io.marcus.application.dto;

public record UpsertBotSubscriberRequest(
        String botId,
        String userId
) {
}
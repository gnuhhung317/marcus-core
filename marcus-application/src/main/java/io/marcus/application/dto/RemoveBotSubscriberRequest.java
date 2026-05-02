package io.marcus.application.dto;

public record RemoveBotSubscriberRequest(
        String botId,
        String userId
) {
}
package io.marcus.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpsertBotSubscriberRequest(
        @NotBlank(message = "botId is required")
        String botId,

        @NotBlank(message = "userId is required")
        String userId
) {
}
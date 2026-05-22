package io.marcus.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpsertUserSessionRequest(
        @NotBlank(message = "userId is required")
        String userId,

        @NotBlank(message = "sessionId is required")
        String sessionId,

        @NotBlank(message = "serverId is required")
        String serverId
) {
}

package io.marcus.application.dto;

public record UpsertUserSessionRequest(
        String userId,
        String sessionId,
        String serverId
) {
}

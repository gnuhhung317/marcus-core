package io.marcus.application.dto;

public record RemoveUserSessionRequest(
        String userId,
        String sessionId
) {
}
package io.marcus.application.usecase;

import io.marcus.application.dto.UpsertUserSessionRequest;
import io.marcus.domain.port.UserSessionRoutingPort;
import org.springframework.stereotype.Service;

@Service
public class UpsertUserSessionUseCase {

    private final UserSessionRoutingPort userSessionRoutingPort;

    public UpsertUserSessionUseCase(UserSessionRoutingPort userSessionRoutingPort) {
        this.userSessionRoutingPort = userSessionRoutingPort;
    }

    public void execute(UpsertUserSessionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Upsert user session request is required");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }
        if (request.sessionId() == null || request.sessionId().isBlank()) {
            throw new IllegalArgumentException("Session id is required");
        }
        if (request.serverId() == null || request.serverId().isBlank()) {
            throw new IllegalArgumentException("Server id is required");
        }

        userSessionRoutingPort.upsertSession(
                request.userId().trim(),
                request.sessionId().trim(),
                request.serverId().trim());
    }
}

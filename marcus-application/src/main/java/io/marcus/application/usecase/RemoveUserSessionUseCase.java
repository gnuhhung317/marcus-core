package io.marcus.application.usecase;

import io.marcus.application.dto.RemoveUserSessionRequest;
import io.marcus.domain.port.UserSessionRoutingPort;
import org.springframework.stereotype.Service;

@Service
public class RemoveUserSessionUseCase {

    private final UserSessionRoutingPort userSessionRoutingPort;

    public RemoveUserSessionUseCase(UserSessionRoutingPort userSessionRoutingPort) {
        this.userSessionRoutingPort = userSessionRoutingPort;
    }

    public void execute(RemoveUserSessionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Remove user session request is required");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }
        if (request.sessionId() == null || request.sessionId().isBlank()) {
            throw new IllegalArgumentException("Session id is required");
        }

        userSessionRoutingPort.removeSession(request.userId().trim(), request.sessionId().trim());
    }
}
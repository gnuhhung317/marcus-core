package io.marcus.application.usecase;

import io.marcus.application.dto.CreateApiKeyRequest;
import io.marcus.application.exception.ResourceConflictException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateCurrentUserApiKeyUseCase {

    private final IdentityService identityService;
    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.CreateApiKeySnapshot execute(CreateApiKeyRequest request) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (request.label() == null || request.label().isBlank()) {
            throw new IllegalArgumentException("API key label is required");
        }

        if ("default".equalsIgnoreCase(request.label().trim())) {
            throw new ResourceConflictException("API key label already exists");
        }

        return terminalReadPort.createCurrentUserApiKey(userId, request.label().trim());
    }
}

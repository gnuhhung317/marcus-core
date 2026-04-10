package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteCurrentUserApiKeyUseCase {

    private final IdentityService identityService;
    private final TerminalReadPort terminalReadPort;

    public void execute(String apiKeyId) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (apiKeyId == null || apiKeyId.isBlank()) {
            throw new IllegalArgumentException("API key id is required");
        }

        terminalReadPort.deleteCurrentUserApiKey(userId, apiKeyId.trim());
    }
}

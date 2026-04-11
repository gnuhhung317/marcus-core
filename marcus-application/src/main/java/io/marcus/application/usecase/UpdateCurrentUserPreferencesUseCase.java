package io.marcus.application.usecase;

import io.marcus.application.dto.UpdateUserPreferencesRequest;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateCurrentUserPreferencesUseCase {

    private final IdentityService identityService;
    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.UserPreferencesSnapshot execute(UpdateUserPreferencesRequest request) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));
        TerminalReadPort.UserPreferencesUpdateSnapshot updateSnapshot
                = new TerminalReadPort.UserPreferencesUpdateSnapshot(
                        request.timezone(),
                        request.locale(),
                        request.emailNotificationsEnabled()
                );
        return terminalReadPort.updateCurrentUserPreferences(userId, updateSnapshot);
    }
}

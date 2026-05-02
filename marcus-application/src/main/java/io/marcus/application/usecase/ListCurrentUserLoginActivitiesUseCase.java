package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListCurrentUserLoginActivitiesUseCase {

    private final IdentityService identityService;
    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.LoginActivityPageSnapshot execute(int page, int size) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));

        return terminalReadPort.listCurrentUserLoginActivities(userId, normalizedPage, normalizedSize);
    }
}

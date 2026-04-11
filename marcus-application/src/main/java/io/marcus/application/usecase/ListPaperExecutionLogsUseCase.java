package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListPaperExecutionLogsUseCase {

    private final IdentityService identityService;
    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.PaperExecutionLogPageSnapshot execute(String cursor, int limit) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        String normalizedCursor = cursor == null || cursor.isBlank() ? null : cursor.trim();

        return terminalReadPort.listPaperExecutionLogs(userId, normalizedCursor, normalizedLimit);
    }
}

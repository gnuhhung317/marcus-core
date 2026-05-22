package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListPaperExecutionLogsUseCase {

    private final IdentityService identityService;
    private final PortfolioReadPort portfolioReadPort;

    public PortfolioReadPort.PaperExecutionLogPageSnapshot execute(String cursor, int limit) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        String normalizedCursor = cursor == null || cursor.isBlank() ? null : cursor.trim();

        return portfolioReadPort.listPaperExecutionLogs(userId, normalizedCursor, normalizedLimit);
    }
}

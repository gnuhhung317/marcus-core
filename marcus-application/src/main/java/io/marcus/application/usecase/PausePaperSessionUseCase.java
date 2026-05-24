package io.marcus.application.usecase;

import io.marcus.domain.exception.ResourceConflictException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PausePaperSessionUseCase {

    private final IdentityService identityService;
    private final PortfolioReadPort portfolioReadPort;

    public PortfolioReadPort.PaperSessionStateSnapshot execute() {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        PortfolioReadPort.PaperSessionSummarySnapshot summary = portfolioReadPort.getPaperSessionSummary(userId);
        if (!"RUNNING".equalsIgnoreCase(summary.status())) {
            throw new ResourceConflictException("Paper session is already paused");
        }

        return portfolioReadPort.pausePaperSession(userId);
    }
}

package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPaperSessionSummaryUseCase {

    private final IdentityService identityService;
    private final PortfolioReadPort portfolioReadPort;

    public PortfolioReadPort.PaperSessionSummarySnapshot execute() {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));
        return portfolioReadPort.getPaperSessionSummary(userId);
    }
}

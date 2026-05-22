package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetDashboardEquitySeriesUseCase {

    private final IdentityService identityService;
    private final PortfolioReadPort portfolioReadPort;

    public List<PortfolioReadPort.TimeSeriesPointSnapshot> execute(String range) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));
        return portfolioReadPort.listDashboardEquitySeries(userId, range);
    }
}

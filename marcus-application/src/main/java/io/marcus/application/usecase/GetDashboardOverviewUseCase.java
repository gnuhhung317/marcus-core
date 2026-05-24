package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.MarketDataReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetDashboardOverviewUseCase {

    private final IdentityService identityService;
    private final MarketDataReadPort marketDataReadPort;

    public MarketDataReadPort.DashboardOverviewSnapshot execute() {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));
        return marketDataReadPort.getDashboardOverview(userId);
    }
}
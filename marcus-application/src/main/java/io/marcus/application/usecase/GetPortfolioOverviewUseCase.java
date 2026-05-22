package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fetch aggregated portfolio metrics for Decision Dashboard header.
 *
 * Aggregates all active subscriptions for current user into consolidated stats:
 * - activeBotsCount: number of active subscriptions - totalSubscribedCapital:
 * user's base capital - aggregateWinRate24h: (successful signals / total
 * signals) across all bots - atRiskSubscriptionCount: subscriptions where
 * drawdown < -10% - totalEquity: starting capital (not formula-based) -
 * aggregateOpenPnL: sum of all open bot P&Ls
 *
 * Used by: Decision Dashboard page /terminal/decision
 */
@Service
@RequiredArgsConstructor
public class GetPortfolioOverviewUseCase {

    private final IdentityService identityService;
    private final PortfolioReadPort portfolioReadPort;

    /**
     * Execute portfolio overview query for current user.
     *
     * @return portfolio overview snapshot with aggregated metrics
     * @throws UnauthenticatedException if user is not authenticated
     */
    public PortfolioReadPort.PortfolioOverviewSnapshot execute() {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));
        return portfolioReadPort.getPortfolioOverview(userId);
    }
}

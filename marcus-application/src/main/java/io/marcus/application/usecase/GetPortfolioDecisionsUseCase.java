package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fetch subscription list enriched with decision reason tags for Decision Dashboard.
 * 
 * Each subscription includes decision context:
 * - currentPnL, drawdownPercent: current performance
 * - winRate, signalCount24h: recent signal stats
 * - reason: enum (SOLID_PERFORMER | NEEDS_REVIEW | HIGH_RISK | SLIPPING)
 * - reasonExplanation: human-readable fact (e.g., "-5% in 7 days")
 * - riskScore: 0.0 (safe) to 1.0 (high risk)
 * 
 * Results are sorted by reason priority: HIGH_RISK first, then NEEDS_REVIEW, SLIPPING, SOLID_PERFORMER.
 * This ensures traders see actionable bots first.
 * 
 * Used by: Decision Dashboard cards /terminal/decision
 */
@Service
@RequiredArgsConstructor
public class GetPortfolioDecisionsUseCase {

    private final IdentityService identityService;
    private final TerminalReadPort terminalReadPort;

    /**
     * Execute portfolio decisions query for current user.
     * @param statusFilter filter subscriptions by status (null=ALL, "ACTIVE", "AT_RISK")
     * @return sorted list of decision-enriched subscriptions
     * @throws UnauthenticatedException if user is not authenticated
     */
    public List<TerminalReadPort.SubscriptionDecisionSnapshot> execute(String statusFilter) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));
        return terminalReadPort.getSubscriptionDecisions(userId, statusFilter);
    }
}

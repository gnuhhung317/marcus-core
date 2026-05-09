package io.marcus.api.controller;

import io.marcus.application.usecase.GetDashboardExchangeAllocationUseCase;
import io.marcus.application.usecase.GetDashboardEquitySeriesUseCase;
import io.marcus.application.usecase.GetDashboardOverviewUseCase;
import io.marcus.application.usecase.GetPortfolioDecisionsUseCase;
import io.marcus.application.usecase.GetPortfolioOverviewUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/dashboard", "/api/dashboard", "/api/v1/dashboard"})
@RequiredArgsConstructor
public class DashboardController {

    private final GetDashboardOverviewUseCase getDashboardOverviewUseCase;
    private final GetDashboardEquitySeriesUseCase getDashboardEquitySeriesUseCase;
    private final GetDashboardExchangeAllocationUseCase getDashboardExchangeAllocationUseCase;
    private final GetPortfolioOverviewUseCase getPortfolioOverviewUseCase;
    private final GetPortfolioDecisionsUseCase getPortfolioDecisionsUseCase;

    @GetMapping("/overview")
    public ResponseEntity<TerminalReadPort.DashboardOverviewSnapshot> getOverview() {
        return ResponseEntity.ok(getDashboardOverviewUseCase.execute());
    }

    @GetMapping("/equity-series")
    public ResponseEntity<List<TerminalReadPort.TimeSeriesPointSnapshot>> getEquitySeries(
            @RequestParam String range
    ) {
        return ResponseEntity.ok(getDashboardEquitySeriesUseCase.execute(range));
    }

    @GetMapping("/exchange-allocation")
    public ResponseEntity<List<TerminalReadPort.ExchangeAllocationSnapshot>> getExchangeAllocation() {
        return ResponseEntity.ok(getDashboardExchangeAllocationUseCase.execute());
    }

    // Pha 1: Decision Dashboard endpoints

    /**
     * GET /api/portfolio/overview
     * Fetch aggregated portfolio metrics for Decision Dashboard header.
     * Used by: Decision Dashboard page header component (PortfolioOverview.tsx)
     * @return portfolio overview snapshot with aggregated metrics
     */
    @GetMapping("/portfolio/overview")
    public ResponseEntity<TerminalReadPort.PortfolioOverviewSnapshot> getPortfolioOverview() {
        return ResponseEntity.ok(getPortfolioOverviewUseCase.execute());
    }

    /**
     * GET /api/portfolio/decisions?status=ALL
     * Fetch subscription list enriched with decision reason tags.
     * Used by: Decision Dashboard cards component (SubscriptionDecisionCards.tsx)
     * @param status filter by status (optional: ALL, ACTIVE, AT_RISK)
     * @return list of decision-enriched subscriptions sorted by reason priority
     */
    @GetMapping("/portfolio/decisions")
    public ResponseEntity<PortfolioDecisionsResponse> getPortfolioDecisions(
            @RequestParam(required = false, defaultValue = "ALL") String status
    ) {
        List<TerminalReadPort.SubscriptionDecisionSnapshot> decisions = getPortfolioDecisionsUseCase.execute(status);
        PortfolioDecisionsResponse response = new PortfolioDecisionsResponse(
                decisions,
                new PortfolioDecisionsResponse.Summary(
                        decisions.size(),
                        (int) decisions.stream().filter(d -> "ACTIVE".equals(d.status())).count(),
                        (int) decisions.stream()
                                .filter(d -> d.reason() == TerminalReadPort.DecisionReason.NEEDS_REVIEW).count(),
                        (int) decisions.stream()
                                .filter(d -> d.reason() == TerminalReadPort.DecisionReason.HIGH_RISK).count()
                )
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Response DTO for portfolio decisions endpoint.
     * Wraps list of decisions with aggregation summary.
     */
    record PortfolioDecisionsResponse(
            List<TerminalReadPort.SubscriptionDecisionSnapshot> decisions,
            Summary summary
    ) {
        record Summary(
                int totalCount,
                int activeCount,
                int reviewNeededCount,
                int highRiskCount
        ) {
        }
    }
}

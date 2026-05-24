package io.marcus.domain.service;

import io.marcus.domain.port.PortfolioReadPort.DecisionReason;
import java.util.List;

/**
 * Pure domain service for analyzing portfolio subscriptions and metrics.
 * Centralizes business rules for determining subscription decision reasons, drawdowns, and PnL.
 */
public final class PortfolioAnalyzerService {

    private PortfolioAnalyzerService() {
        // Utility class
    }

    public static double calculateDrawdown(List<SignalMetricsCalculator.SignalData> signals) {
        if (signals == null || signals.isEmpty()) {
            return 0.0;
        }
        double minValue = signals.stream()
                .mapToDouble(SignalMetricsCalculator::deriveReturn)
                .min()
                .orElse(0.0);
        return Math.min(0.0, minValue);
    }

    public static double calculateCurrentPnL(List<SignalMetricsCalculator.SignalData> signals, double totalCapital) {
        if (signals == null || signals.isEmpty()) {
            return 0.0;
        }
        double totalReturn = signals.stream()
                .mapToDouble(SignalMetricsCalculator::deriveReturn)
                .sum();
        return SignalMetricsCalculator.round2(totalReturn * totalCapital);
    }

    public static DecisionReason determineReason(
            double winRate,
            double drawdown,
            boolean hasRecentSignals,
            double failureRate,
            double maxDrawdownThreshold,
            double mediumRiskThreshold
    ) {
        double highRiskThresh = -Math.abs(maxDrawdownThreshold);
        double medRiskThresh = -Math.abs(mediumRiskThreshold);

        if (drawdown < highRiskThresh) {
            return DecisionReason.HIGH_RISK;
        }
        if (drawdown < medRiskThresh || failureRate > 0.20) {
            return DecisionReason.NEEDS_REVIEW;
        }
        if (!hasRecentSignals) {
            return DecisionReason.SLIPPING;
        }
        if (winRate > 0.60) {
            return DecisionReason.SOLID_PERFORMER;
        }
        return DecisionReason.NEEDS_REVIEW;
    }

    public static String generateReasonExplanation(DecisionReason reason, double winRate, double drawdown) {
        return switch (reason) {
            case SOLID_PERFORMER -> String.format("↑%.1f%% win rate", winRate * 100);
            case NEEDS_REVIEW -> String.format("%.0f%% drawdown in 7 days", drawdown * 100);
            case HIGH_RISK -> String.format("%.0f%% critical drawdown", drawdown * 100);
            case SLIPPING -> "No recent signals";
        };
    }
}

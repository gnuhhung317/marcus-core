package io.marcus.domain.service;

import io.marcus.domain.vo.SignalAction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pure domain service for computing trading signal performance metrics.
 * Stateless, no infrastructure or framework dependencies.
 *
 * <p>All financial metric calculations that were previously embedded in
 * {@code StaticTerminalReadAdapter} (infrastructure layer) are centralized here
 * to respect Clean Architecture boundaries.</p>
 */
public final class SignalMetricsCalculator {

    private SignalMetricsCalculator() {
        // Utility class — no instantiation
    }

    /**
     * Minimal signal data required for metric computation.
     * Decouples calculation from JPA entities.
     */
    public record SignalData(
            BigDecimal entry,
            BigDecimal takeProfit,
            BigDecimal stopLoss,
            SignalAction action
    ) {

    }

    /**
     * Aggregated performance metrics for a set of signals.
     */
    public record MetricsResult(
            double annualReturn,
            double maxDrawdown,
            double sharpe,
            double winRate,
            double avgTradeReturn,
            double tradesPerDay,
            String risk
    ) {

    }

    /**
     * Derive the percentage return of a single signal.
     *
     * <p>Uses {@code takeProfit} as the reference exit price when available,
     * falling back to {@code stopLoss}. Direction is inferred from the
     * signal action (SHORT actions invert the return).</p>
     *
     * @param signal the signal data
     * @return percentage return (e.g., 0.05 = 5%), or 0.0 if data is incomplete
     */
    public static double deriveReturn(SignalData signal) {
        double entry = toDouble(signal.entry());
        if (entry == 0.0d) {
            return 0.0d;
        }

        double referencePrice = toDouble(signal.takeProfit());
        if (referencePrice == 0.0d) {
            referencePrice = toDouble(signal.stopLoss());
        }
        if (referencePrice == 0.0d) {
            return 0.0d;
        }

        double direction = isShortAction(signal.action()) ? -1.0d : 1.0d;
        return round4(((referencePrice - entry) / Math.abs(entry)) * direction);
    }

    /**
     * Derive the drawdown risk of a single signal based on entry vs stop-loss distance.
     *
     * @param signal the signal data
     * @return drawdown as a positive fraction (e.g., 0.05 = 5% drawdown), or 0.0
     */
    public static double deriveDrawdown(SignalData signal) {
        double entry = toDouble(signal.entry());
        double stopLoss = toDouble(signal.stopLoss());
        if (entry == 0.0d || stopLoss == 0.0d) {
            return 0.0d;
        }
        return round4(Math.max(0.0d, Math.abs(entry - stopLoss) / Math.abs(entry)));
    }

    /**
     * Calculate aggregated performance metrics from a list of signals.
     *
     * @param signals list of signal data points
     * @param ageDays number of calendar days the signals span (min 1)
     * @return aggregated metrics result
     */
    public static MetricsResult calculate(List<SignalData> signals, long ageDays) {
        if (signals == null || signals.isEmpty()) {
            return new MetricsResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, classifyRisk(0.0, 0.0));
        }

        double annualReturn = round4(
                signals.stream().mapToDouble(SignalMetricsCalculator::deriveReturn).average().orElse(0.0)
        );
        double maxDrawdown = round4(
                signals.stream().mapToDouble(SignalMetricsCalculator::deriveDrawdown).max().orElse(0.0)
        );
        long profitableSignals = signals.stream()
                .filter(s -> deriveReturn(s) > 0)
                .count();
        double winRate = round4(profitableSignals / (double) signals.size());
        double avgTradeReturn = annualReturn; // same as annualReturn for simplified model
        long normalizedAgeDays = Math.max(1L, ageDays);
        double tradesPerDay = round4(signals.size() / (double) normalizedAgeDays);
        double sharpe = round4(annualReturn / Math.max(0.01, maxDrawdown + 0.01));

        return new MetricsResult(
                annualReturn, maxDrawdown, sharpe, winRate,
                avgTradeReturn, tradesPerDay, classifyRisk(annualReturn, maxDrawdown)
        );
    }

    /**
     * Classify risk level based on return and drawdown thresholds.
     *
     * @param annualReturn the annualized return
     * @param maxDrawdown  the maximum drawdown
     * @return "HIGH", "MEDIUM", or "LOW"
     */
    public static String classifyRisk(double annualReturn, double maxDrawdown) {
        if (maxDrawdown >= 0.25d || annualReturn <= 0.05d) {
            return "HIGH";
        }
        if (maxDrawdown >= 0.12d) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * Round a value to 4 decimal places.
     */
    public static double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    /**
     * Round a value to 2 decimal places.
     */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static double toDouble(BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }

    private static boolean isShortAction(SignalAction action) {
        return action == SignalAction.OPEN_SHORT || action == SignalAction.CLOSE_SHORT;
    }
}

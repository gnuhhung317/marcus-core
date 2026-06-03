package io.marcus.domain.service;

import io.marcus.domain.model.BotDryRunPortfolioPoint;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calculates performance metrics from equity curve points. Uses correct
 * financial formulas for CAGR, Sharpe ratio, and max drawdown.
 *
 * <p>
 * Key formulas:</p>
 * <ul>
 * <li>CAGR = (Ending Value / Beginning Value)^(365/Days) - 1</li>
 * <li>Annualized Volatility = Daily Std Dev × √365 (for 24/7 crypto
 * markets)</li>
 * <li>Sharpe Ratio = (CAGR - Risk Free Rate) / Annualized Volatility</li>
 * <li>Max Drawdown = (Peak - Trough) / Peak</li>
 * </ul>
 */
@Value
public class EquityCurveMetricsCalculator {

    private static final double RISK_FREE_RATE = 0.0; // Can be parameterized for traditional markets

    /**
     * Calculate performance metrics from a list of portfolio points.
     *
     * @param points List of equity curve points, sorted by timestamp
     * @return MetricsResult containing CAGR, max drawdown, Sharpe ratio, and
     * sample days
     */
    public MetricsResult calculate(List<BotDryRunPortfolioPoint> points) {
        if (points == null || points.size() < 2) {
            return new MetricsResult(0.0, 0.0, 0.0, 0);
        }

        // Sort by timestamp to ensure correct order
        List<BotDryRunPortfolioPoint> sorted = points.stream()
                .sorted(Comparator.comparing(BotDryRunPortfolioPoint::timestamp))
                .toList();

        double firstEquity = sorted.get(0).equity().doubleValue();
        double lastEquity = sorted.get(sorted.size() - 1).equity().doubleValue();

        // Validate first equity to avoid division by zero
        if (firstEquity <= 0) {
            return new MetricsResult(0.0, 0.0, 0.0, 0);
        }

        LocalDateTime firstTimestamp = sorted.get(0).timestamp();
        LocalDateTime lastTimestamp = sorted.get(sorted.size() - 1).timestamp();
        long sampleDays = Math.max(1, ChronoUnit.DAYS.between(firstTimestamp, lastTimestamp) + 1);

        //  CORRECT CAGR: Compound Annual Growth Rate
        // Formula: (Ending / Beginning)^(365/Days) - 1
        double cagr = Math.pow((lastEquity / firstEquity), (365.0 / sampleDays)) - 1.0;

        //  CORRECT MAX DRAWDOWN
        double maxDrawdown = calculateMaxDrawdown(sorted);

        //  CORRECT ANNUALIZED VOLATILITY
        double annualizedVol = calculateAnnualizedVolatility(sorted);

        //  CORRECT SHARPE RATIO
        // Formula: (Portfolio Return - Risk Free Rate) / Portfolio Volatility
        double sharpe = annualizedVol == 0.0 ? 0.0 : (cagr - RISK_FREE_RATE) / annualizedVol;

        return new MetricsResult(cagr, -Math.abs(maxDrawdown), sharpe, sampleDays);
    }

    /**
     * Calculate maximum drawdown as percentage from peak to trough.
     *
     * @param points Sorted list of portfolio points
     * @return Maximum drawdown as a positive decimal (e.g., 0.15 = 15%
     * drawdown)
     */
    private double calculateMaxDrawdown(List<BotDryRunPortfolioPoint> points) {
        double peak = points.get(0).equity().doubleValue();
        double maxDD = 0.0;

        for (BotDryRunPortfolioPoint point : points) {
            double equity = point.equity().doubleValue();
            peak = Math.max(peak, equity);

            // Drawdown = (Peak - Current) / Peak
            if (peak > 0) {
                double drawdown = (peak - equity) / peak;
                maxDD = Math.max(maxDD, drawdown);
            }
        }

        return maxDD;
    }

    /**
     * Calculate annualized volatility (standard deviation of daily returns).
     * For crypto markets (24/7), we annualize using √365.
     *
     * @param points Sorted list of portfolio points
     * @return Annualized volatility as a decimal
     */
    private double calculateAnnualizedVolatility(List<BotDryRunPortfolioPoint> points) {
        List<Double> dailyReturns = new ArrayList<>();

        for (int i = 1; i < points.size(); i++) {
            double prevEquity = points.get(i - 1).equity().doubleValue();
            double currEquity = points.get(i).equity().doubleValue();

            // Daily return = (Current - Previous) / Previous
            double dailyReturn = (currEquity - prevEquity) / prevEquity;
            dailyReturns.add(dailyReturn);
        }

        if (dailyReturns.isEmpty()) {
            return 0.0;
        }

        // Calculate mean of daily returns
        double mean = dailyReturns.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        // Calculate variance
        double variance = dailyReturns.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);

        // Daily volatility = sqrt(variance)
        double dailyVol = Math.sqrt(variance);

        //  CORRECT: Annualize for 24/7 crypto market using √365
        return dailyVol * Math.sqrt(365.0);
    }

    /**
     * Result record containing calculated metrics.
     */
    public record MetricsResult(
            double annualReturn, // CAGR (Compound Annual Growth Rate)
            double maxDrawdown, // Maximum drawdown (negative value)
            double sharpe, // Sharpe ratio
            long sampleDays // Number of days in sample period
            ) {

    }
}

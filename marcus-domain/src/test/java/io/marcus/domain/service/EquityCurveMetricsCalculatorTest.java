package io.marcus.domain.service;

import io.marcus.domain.model.BotDryRunPortfolioPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EquityCurveMetricsCalculatorTest {

    private final EquityCurveMetricsCalculator calculator = new EquityCurveMetricsCalculator();

    @Test
    @DisplayName("Should return zeros for null points")
    void shouldReturnZerosForNullPoints() {
        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(null);
        assertEquals(0.0, result.annualReturn());
        assertEquals(0.0, result.maxDrawdown());
        assertEquals(0.0, result.sharpe());
        assertEquals(0, result.sampleDays());
    }

    @Test
    @DisplayName("Should return zeros for single point")
    void shouldReturnZerosForSinglePoint() {
        List<BotDryRunPortfolioPoint> points = List.of(
                point(10000.0, LocalDateTime.now())
        );
        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);
        assertEquals(0.0, result.annualReturn());
        assertEquals(0.0, result.maxDrawdown());
        assertEquals(0.0, result.sharpe());
        assertEquals(0, result.sampleDays());
    }

    @Test
    @DisplayName("Should return zeros when first equity is zero")
    void shouldReturnZerosWhenFirstEquityIsZero() {
        LocalDateTime now = LocalDateTime.now();
        List<BotDryRunPortfolioPoint> points = List.of(
                point(0.0, now),
                point(100.0, now.plusDays(1))
        );
        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);
        assertEquals(0.0, result.annualReturn());
        assertEquals(0.0, result.maxDrawdown());
        assertEquals(0.0, result.sharpe());
        assertEquals(0, result.sampleDays());
    }

    @Test
    @DisplayName("Should return zeros when first equity is negative")
    void shouldReturnZerosWhenFirstEquityIsNegative() {
        LocalDateTime now = LocalDateTime.now();
        List<BotDryRunPortfolioPoint> points = List.of(
                point(-100.0, now),
                point(100.0, now.plusDays(1))
        );
        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);
        assertEquals(0.0, result.annualReturn());
        assertEquals(0.0, result.maxDrawdown());
        assertEquals(0.0, result.sharpe());
        assertEquals(0, result.sampleDays());
    }

    @Test
    @DisplayName("Should calculate CAGR correctly for steady growth")
    void shouldCalculateCagrForSteadyGrowth() {
        LocalDateTime now = LocalDateTime.now();
        // 10000 -> 11000 over 365 days = 10% return
        List<BotDryRunPortfolioPoint> points = List.of(
                point(10000.0, now),
                point(11000.0, now.plusDays(365))
        );

        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);

        // sampleDays = 365 + 1 = 366 (inclusive)
        // CAGR = (11000/10000)^(365/366) - 1 = 1.1^0.99727 - 1 ≈ 0.0997136
        assertEquals(0.099714, result.annualReturn(), 0.0001);
        assertEquals(366, result.sampleDays());
    }

    @Test
    @DisplayName("Should calculate CAGR correctly for multi-year growth")
    void shouldCalculateCagrForMultiYearGrowth() {
        LocalDateTime now = LocalDateTime.now();
        // 10000 -> 12100 over 2 years = (12100/10000)^(365/730) - 1 = 0.10
        List<BotDryRunPortfolioPoint> points = List.of(
                point(10000.0, now),
                point(12100.0, now.plusDays(730))
        );

        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);

        // sampleDays = 730 + 1 = 731 (inclusive)
        // CAGR = (12100/10000)^(365/731) - 1 = 1.21^0.4993 - 1 ≈ 0.0998566
        assertEquals(0.099857, result.annualReturn(), 0.0001);
    }

    @Test
    @DisplayName("Should calculate max drawdown correctly")
    void shouldCalculateMaxDrawdown() {
        LocalDateTime now = LocalDateTime.now();
        // Equity goes: 10000 -> 12000 (peak) -> 9000 (trough) -> 11000
        // Max drawdown = (12000 - 9000) / 12000 = 0.25
        List<BotDryRunPortfolioPoint> points = List.of(
                point(10000.0, now),
                point(12000.0, now.plusDays(10)),
                point(9000.0, now.plusDays(20)),
                point(11000.0, now.plusDays(30))
        );

        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);

        // maxDrawdown should be negative (convention)
        assertEquals(-0.25, result.maxDrawdown(), 0.0001);
    }

    @Test
    @DisplayName("Should return zero drawdown for monotonically increasing equity")
    void shouldReturnZeroDrawdownForMonotonicallyIncreasing() {
        LocalDateTime now = LocalDateTime.now();
        List<BotDryRunPortfolioPoint> points = List.of(
                point(10000.0, now),
                point(10500.0, now.plusDays(10)),
                point(11000.0, now.plusDays(20))
        );

        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);

        assertEquals(0.0, result.maxDrawdown(), 0.0001);
    }

    @Test
    @DisplayName("Should calculate annualized volatility correctly")
    void shouldCalculateAnnualizedVolatility() {
        LocalDateTime now = LocalDateTime.now();
        // Two equal daily returns of 1%: (10100-10000)/10000 = 0.01
        List<BotDryRunPortfolioPoint> points = List.of(
                point(10000.0, now),
                point(10100.0, now.plusDays(1)),
                point(10201.0, now.plusDays(2))
        );

        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);

        // Daily returns: [0.01, 0.01]
        // Mean = 0.01
        // Variance = ((0.01-0.01)^2 + (0.01-0.01)^2) / 2 = 0
        // Daily vol = 0
        // Annualized = 0 * sqrt(365) = 0
        // Sharpe = (CAGR - 0) / 0 = 0 (handled by zero guard)
        assertEquals(0.0, result.sharpe(), 0.0001);
    }

    @Test
    @DisplayName("Should calculate Sharpe ratio correctly")
    void shouldCalculateSharpeRatio() {
        LocalDateTime now = LocalDateTime.now();
        // Create a scenario with known values
        // 10000 -> 10050 (+0.5%) -> 9950 (-0.995%) -> 10100 (+1.51%)
        List<BotDryRunPortfolioPoint> points = List.of(
                point(10000.0, now),
                point(10050.0, now.plusDays(1)),
                point(9950.0, now.plusDays(2)),
                point(10100.0, now.plusDays(3))
        );

        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);

        // Daily returns: [0.005, -0.00995, 0.01508]
        // Mean ≈ 0.00338
        // Variance = ((0.005-0.00338)^2 + (-0.00995-0.00338)^2 + (0.01508-0.00338)^2) / 3
        //          = (0.00000262 + 0.0001778 + 0.0001369) / 3
        //          = 0.0003173 / 3
        //          = 0.0001058
        // Daily vol = sqrt(0.0001058) ≈ 0.01028
        // Annualized vol = 0.01028 * sqrt(365) ≈ 0.1964
        // CAGR over 4 days = (10100/10000)^(365/4) - 1
        // CAGR = 1.01^91.25 - 1 ≈ 2.48 - 1 = 1.48 (very high due to short period)
        // Sharpe = 1.48 / 0.1964 ≈ 7.54
        assertTrue(result.annualReturn() > 0);
        assertTrue(result.sharpe() > 0);
    }

    @Test
    @DisplayName("Should handle unsorted points by sorting by timestamp")
    void shouldHandleUnsortedPoints() {
        LocalDateTime now = LocalDateTime.now();
        // Points in reverse order
        List<BotDryRunPortfolioPoint> points = List.of(
                point(11000.0, now.plusDays(30)),
                point(9000.0, now.plusDays(20)),
                point(10000.0, now)
        );

        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);

        // Should not crash, should produce reasonable results
        // After sorting: 10000 -> 9000 (DD=10%) -> 11000 (new peak)
        // Max drawdown = (10000 - 9000) / 10000 = 0.10
        assertEquals(-0.10, result.maxDrawdown(), 0.001);
    }

    @Test
    @DisplayName("Should return zero volatility for flat equity curve")
    void shouldReturnZeroVolatilityForFlatCurve() {
        LocalDateTime now = LocalDateTime.now();
        List<BotDryRunPortfolioPoint> points = List.of(
                point(10000.0, now),
                point(10000.0, now.plusDays(1)),
                point(10000.0, now.plusDays(2)),
                point(10000.0, now.plusDays(3))
        );

        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);

        // CAGR = (10000/10000)^(365/4) - 1 = 0
        assertEquals(0.0, result.annualReturn(), 0.0001);
        assertEquals(0.0, result.sharpe(), 0.0001);
    }

    @Test
    @DisplayName("Should handle large drawdown correctly")
    void shouldHandleLargeDrawdown() {
        LocalDateTime now = LocalDateTime.now();
        // Equity goes: 10000 -> 2000 (80% drawdown) -> 10000
        List<BotDryRunPortfolioPoint> points = List.of(
                point(10000.0, now),
                point(2000.0, now.plusDays(10)),
                point(10000.0, now.plusDays(20))
        );

        EquityCurveMetricsCalculator.MetricsResult result = calculator.calculate(points);

        // Max drawdown = (10000 - 2000) / 10000 = 0.80
        assertEquals(-0.80, result.maxDrawdown(), 0.0001);
    }

    @Test
    @DisplayName("Should use sample days correctly for CAGR")
    void shouldUseSampleDaysForCagr() {
        LocalDateTime now = LocalDateTime.now();
        // Same return but over different periods
        // 10000 -> 10500 over 30 days
        List<BotDryRunPortfolioPoint> points30 = List.of(
                point(10000.0, now),
                point(10500.0, now.plusDays(30))
        );

        // 10000 -> 10500 over 60 days
        List<BotDryRunPortfolioPoint> points60 = List.of(
                point(10000.0, now),
                point(10500.0, now.plusDays(60))
        );

        EquityCurveMetricsCalculator.MetricsResult result30 = calculator.calculate(points30);
        EquityCurveMetricsCalculator.MetricsResult result60 = calculator.calculate(points60);

        // CAGR should be higher for shorter period with same return
        assertTrue(result30.annualReturn() > result60.annualReturn(),
                "CAGR should be higher for shorter period with same return");
    }

    private BotDryRunPortfolioPoint point(double equity, LocalDateTime timestamp) {
        return new BotDryRunPortfolioPoint(
                "test-bot",
                timestamp,
                BigDecimal.ZERO,
                BigDecimal.valueOf(equity),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}

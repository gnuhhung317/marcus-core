package io.marcus.domain.service;

import io.marcus.domain.port.PortfolioReadPort.DecisionReason;
import io.marcus.domain.vo.SignalAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioAnalyzerServiceTest {

    @Test
    @DisplayName("Should calculate drawdown correctly based on signals return")
    void shouldCalculateDrawdown() {
        SignalMetricsCalculator.SignalData s1 = new SignalMetricsCalculator.SignalData(
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("95"), SignalAction.OPEN_LONG
        ); // Return: 0.05
        SignalMetricsCalculator.SignalData s2 = new SignalMetricsCalculator.SignalData(
                new BigDecimal("100"), new BigDecimal("92"), new BigDecimal("90"), SignalAction.OPEN_LONG
        ); // Return: -0.08
        SignalMetricsCalculator.SignalData s3 = new SignalMetricsCalculator.SignalData(
                new BigDecimal("100"), new BigDecimal("102"), new BigDecimal("98"), SignalAction.OPEN_LONG
        ); // Return: 0.02

        double drawdown = PortfolioAnalyzerService.calculateDrawdown(List.of(s1, s2, s3));
        assertEquals(-0.08, drawdown, 0.0001);
    }

    @Test
    @DisplayName("Should return 0.0 drawdown when signals are empty")
    void shouldReturnZeroDrawdownWhenEmpty() {
        double drawdown = PortfolioAnalyzerService.calculateDrawdown(List.of());
        assertEquals(0.0, drawdown);
    }

    @Test
    @DisplayName("Should calculate current PnL correctly based on total capital")
    void shouldCalculateCurrentPnL() {
        SignalMetricsCalculator.SignalData s1 = new SignalMetricsCalculator.SignalData(
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("95"), SignalAction.OPEN_LONG
        ); // Return: 0.05
        SignalMetricsCalculator.SignalData s2 = new SignalMetricsCalculator.SignalData(
                new BigDecimal("100"), new BigDecimal("95"), new BigDecimal("90"), SignalAction.OPEN_LONG
        ); // Return: -0.05

        // Total return = 0.05 - 0.05 = 0.0
        double pnl = PortfolioAnalyzerService.calculateCurrentPnL(List.of(s1, s2), 10000.0);
        assertEquals(0.0, pnl, 0.0001);

        // One winning signal
        double pnlWinner = PortfolioAnalyzerService.calculateCurrentPnL(List.of(s1), 10000.0);
        assertEquals(500.0, pnlWinner, 0.0001);
    }

    @Test
    @DisplayName("Should determine HIGH_RISK reason if drawdown exceeds max threshold")
    void shouldDetermineHighRiskReason() {
        DecisionReason reason = PortfolioAnalyzerService.determineReason(
                0.70, -0.15, true, 0.10, 0.10, 0.05
        );
        assertEquals(DecisionReason.HIGH_RISK, reason);
    }

    @Test
    @DisplayName("Should determine NEEDS_REVIEW reason if drawdown exceeds medium threshold or failure rate > 20%")
    void shouldDetermineNeedsReviewReason() {
        DecisionReason reasonDrawdown = PortfolioAnalyzerService.determineReason(
                0.70, -0.07, true, 0.10, 0.10, 0.05
        );
        assertEquals(DecisionReason.NEEDS_REVIEW, reasonDrawdown);

        DecisionReason reasonFailure = PortfolioAnalyzerService.determineReason(
                0.70, -0.02, true, 0.25, 0.10, 0.05
        );
        assertEquals(DecisionReason.NEEDS_REVIEW, reasonFailure);
    }

    @Test
    @DisplayName("Should determine SLIPPING reason if no recent signals")
    void shouldDetermineSlippingReason() {
        DecisionReason reason = PortfolioAnalyzerService.determineReason(
                0.70, -0.01, false, 0.10, 0.10, 0.05
        );
        assertEquals(DecisionReason.SLIPPING, reason);
    }

    @Test
    @DisplayName("Should determine SOLID_PERFORMER reason if win rate > 60% and not risk/slipping")
    void shouldDetermineSolidPerformerReason() {
        DecisionReason reason = PortfolioAnalyzerService.determineReason(
                0.65, -0.01, true, 0.10, 0.10, 0.05
        );
        assertEquals(DecisionReason.SOLID_PERFORMER, reason);
    }
}

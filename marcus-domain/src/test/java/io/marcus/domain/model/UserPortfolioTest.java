package io.marcus.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UserPortfolioTest {

    @Test
    void testCreateDefault() {
        UserPortfolio portfolio = UserPortfolio.createDefault("user-1");
        assertNotNull(portfolio);
        assertEquals("user-1", portfolio.getUserId());
        assertEquals(new BigDecimal("10000"), portfolio.getTotalCapital());
        assertEquals(new BigDecimal("10000"), portfolio.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, portfolio.getRealizedPnl());
        assertEquals(BigDecimal.ZERO, portfolio.getUnrealizedPnl());
        assertEquals(new BigDecimal("0.1000"), portfolio.getMaxDrawdownThreshold());
        assertEquals(new BigDecimal("0.0500"), portfolio.getMediumRiskThreshold());
    }

    @Test
    void testUpdateBalance() {
        UserPortfolio portfolio = UserPortfolio.createDefault("user-1");
        portfolio.updateBalance(new BigDecimal("15000"), new BigDecimal("12000"), new BigDecimal("-500"), "binance");
        assertEquals(new BigDecimal("15000"), portfolio.getTotalCapital());
        assertEquals(new BigDecimal("12000"), portfolio.getAvailableBalance());
        assertEquals(new BigDecimal("-500"), portfolio.getUnrealizedPnl());
        assertEquals("binance", portfolio.getExchangeId());
        assertNotNull(portfolio.getLastSyncAt());
    }

    @Test
    void testIsHighRiskAndMediumRisk() {
        UserPortfolio portfolio = UserPortfolio.createDefault("user-1");

        // default thresholds: maxDrawdownThreshold = 10% (0.1000), mediumRiskThreshold = 5% (0.0500)
        // Scenario 1: Zero unrealized PnL (no drawdown)
        assertFalse(portfolio.isHighRisk());
        assertFalse(portfolio.isMediumRisk());

        // Scenario 2: Positive unrealized PnL (gain)
        portfolio.setUnrealizedPnl(new BigDecimal("500"));
        assertFalse(portfolio.isHighRisk());
        assertFalse(portfolio.isMediumRisk());

        // Scenario 3: Negative unrealized PnL within medium risk (e.g. 4% loss -> 400 USDT drawdown on 10000 USDT)
        portfolio.setUnrealizedPnl(new BigDecimal("-400"));
        assertFalse(portfolio.isHighRisk());
        assertFalse(portfolio.isMediumRisk());

        // Scenario 4: Drawdown equals/exceeds medium risk but below high risk (e.g. 6% loss -> 600 USDT)
        portfolio.setUnrealizedPnl(new BigDecimal("-600"));
        assertFalse(portfolio.isHighRisk());
        assertTrue(portfolio.isMediumRisk());

        // Scenario 5: Drawdown equals/exceeds high risk (e.g. 11% loss -> 1100 USDT)
        portfolio.setUnrealizedPnl(new BigDecimal("-1100"));
        assertTrue(portfolio.isHighRisk());
        assertTrue(portfolio.isMediumRisk());
    }

    @Test
    void testRiskChecksWithEdgeCases() {
        UserPortfolio portfolio = UserPortfolio.createDefault("user-1");

        // Edge case: totalCapital is null
        portfolio.setTotalCapital(null);
        portfolio.setUnrealizedPnl(new BigDecimal("-1000"));
        assertFalse(portfolio.isHighRisk());
        assertFalse(portfolio.isMediumRisk());

        // Edge case: totalCapital is zero
        portfolio.setTotalCapital(BigDecimal.ZERO);
        assertFalse(portfolio.isHighRisk());
        assertFalse(portfolio.isMediumRisk());

        // Edge case: totalCapital is negative
        portfolio.setTotalCapital(new BigDecimal("-100"));
        assertFalse(portfolio.isHighRisk());
        assertFalse(portfolio.isMediumRisk());

        // Edge case: thresholds are null
        portfolio.setTotalCapital(new BigDecimal("10000"));
        portfolio.setMaxDrawdownThreshold(null);
        portfolio.setMediumRiskThreshold(null);
        assertFalse(portfolio.isHighRisk());
        assertFalse(portfolio.isMediumRisk());
    }
}

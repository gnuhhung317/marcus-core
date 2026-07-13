package io.marcus.api.controller;

import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.application.usecase.GetDashboardEquitySeriesUseCase;
import io.marcus.application.usecase.GetDashboardExchangeAllocationUseCase;
import io.marcus.application.usecase.GetDashboardOverviewUseCase;
import io.marcus.application.usecase.GetPortfolioDecisionsUseCase;
import io.marcus.application.usecase.GetPortfolioOverviewUseCase;
import io.marcus.application.usecase.GetDashboardTradesUseCase;
import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.port.MarketDataReadPort;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.infrastructure.security.BotSignatureInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetDashboardOverviewUseCase getDashboardOverviewUseCase;

    @MockBean
    private GetDashboardEquitySeriesUseCase getDashboardEquitySeriesUseCase;

    @MockBean
    private GetDashboardExchangeAllocationUseCase getDashboardExchangeAllocationUseCase;

    @MockBean
    private GetPortfolioOverviewUseCase getPortfolioOverviewUseCase;

    @MockBean
    private GetPortfolioDecisionsUseCase getPortfolioDecisionsUseCase;

    @MockBean
    private GetDashboardTradesUseCase getDashboardTradesUseCase;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @BeforeEach
    void allowSignatureInterceptor() throws Exception {
        doAnswer(invocation -> true)
                .when(botSignatureInterceptor)
                .preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any());
    }

    @Test
    void shouldGetDashboardOverview() throws Exception {
        LocalDateTime lastUpdated = LocalDateTime.of(2026, 4, 3, 9, 15);
        when(getDashboardOverviewUseCase.execute())
                .thenReturn(new MarketDataReadPort.DashboardOverviewSnapshot(12500.25, 132.4, 0.61, 3, 0, 0, "FRESH", lastUpdated));

        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEquity").value(12500.25))
                .andExpect(jsonPath("$.activeBots").value(3))
                .andExpect(jsonPath("$.lastUpdated").value("2026-04-03T09:15:00"));
    }

    @Test
    void shouldGetDashboardEquitySeries() throws Exception {
        when(getDashboardEquitySeriesUseCase.execute("1M"))
                .thenReturn(List.of(
                        new PortfolioReadPort.TimeSeriesPointSnapshot(LocalDateTime.of(2026, 4, 1, 10, 0), 100.5),
                        new PortfolioReadPort.TimeSeriesPointSnapshot(LocalDateTime.of(2026, 4, 2, 10, 0), 101.5)
                ));

        mockMvc.perform(get("/api/v1/dashboard/equity-series").param("range", "1M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(100.5));
    }

    @Test
    void shouldGetDashboardExchangeAllocation() throws Exception {
        when(getDashboardExchangeAllocationUseCase.execute())
                .thenReturn(List.of(new MarketDataReadPort.ExchangeAllocationSnapshot("BINANCE", 42.0)));

        mockMvc.perform(get("/api/v1/dashboard/exchange-allocation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exchange").value("BINANCE"))
                .andExpect(jsonPath("$[0].percentage").value(42.0));
    }

    @Test
    void shouldReturnBadRequestWhenEquityRangeIsInvalid() throws Exception {
        when(getDashboardEquitySeriesUseCase.execute("bad"))
                .thenThrow(new IllegalArgumentException("Unsupported range: bad"));

        mockMvc.perform(get("/api/v1/dashboard/equity-series").param("range", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnUnauthorizedWhenDashboardOverviewUnauthenticated() throws Exception {
        when(getDashboardOverviewUseCase.execute())
                .thenThrow(new UnauthenticatedException("No authenticated user found"));

        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
     }

    @Test
    void shouldGetDashboardTrades() throws Exception {
        when(getDashboardTradesUseCase.execute(0, 8, null))
                .thenReturn(new BotDiscoveryReadPort.TradeLogPageSnapshot(List.of(), 0, 8, 0));

        mockMvc.perform(get("/api/v1/dashboard/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldReturnUnauthorizedWhenDashboardTradesUnauthenticated() throws Exception {
        when(getDashboardTradesUseCase.execute(0, 8, null))
                .thenThrow(new UnauthenticatedException("No authenticated user found"));

        mockMvc.perform(get("/api/v1/dashboard/trades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldGetPortfolioDecisionsWithRiskSummary() throws Exception {
        when(getPortfolioDecisionsUseCase.execute("ALL"))
                .thenReturn(List.of(
                        new PortfolioReadPort.SubscriptionDecisionSnapshot(
                                "sub-risk", "bot-risk", "BTC Sentinel", "", "ACTIVE",
                                -120.0, -0.03, -0.22,
                                PortfolioReadPort.DecisionReason.HIGH_RISK,
                                "Critical drawdown", 0.91, 10, LocalDateTime.of(2026, 4, 3, 9, 55), "FRESH",
                                LocalDateTime.of(2026, 4, 3, 10, 0), "BINANCE"
                        ),
                        new PortfolioReadPort.SubscriptionDecisionSnapshot(
                                "sub-review", "bot-review", "ETH Momentum", "", "ACTIVE",
                                80.0, 0.02, -0.08,
                                PortfolioReadPort.DecisionReason.NEEDS_REVIEW,
                                "Needs review", 0.58, 7, LocalDateTime.of(2026, 4, 3, 10, 45), "STALE",
                                LocalDateTime.of(2026, 4, 3, 11, 0), "BYBIT"
                        ),
                        new PortfolioReadPort.SubscriptionDecisionSnapshot(
                                "sub-solid", "bot-solid", "SOL Trend", "", "ACTIVE",
                                220.0, 0.06, -0.02,
                                PortfolioReadPort.DecisionReason.SOLID_PERFORMER,
                                "Stable execution", 0.14, 30, LocalDateTime.of(2026, 4, 3, 11, 55), "FRESH",
                                LocalDateTime.of(2026, 4, 3, 12, 0), "OKX"
                        )
                ));

        mockMvc.perform(get("/api/v1/dashboard/portfolio/decisions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCount").value(3))
                .andExpect(jsonPath("$.summary.activeCount").value(1))
                .andExpect(jsonPath("$.summary.reviewNeededCount").value(1))
                .andExpect(jsonPath("$.summary.highRiskCount").value(1));
    }

    @Test
    void shouldPassPortfolioDecisionStatusFilter() throws Exception {
        when(getPortfolioDecisionsUseCase.execute("AT_RISK")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dashboard/portfolio/decisions").param("status", "AT_RISK"))
                .andExpect(status().isOk());

        verify(getPortfolioDecisionsUseCase).execute("AT_RISK");
    }
}

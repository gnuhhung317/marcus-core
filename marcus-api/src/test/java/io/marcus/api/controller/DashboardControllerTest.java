package io.marcus.api.controller;

import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.application.usecase.GetDashboardEquitySeriesUseCase;
import io.marcus.application.usecase.GetDashboardExchangeAllocationUseCase;
import io.marcus.application.usecase.GetDashboardOverviewUseCase;
import io.marcus.domain.port.TerminalReadPort;
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
        when(getDashboardOverviewUseCase.execute())
                .thenReturn(new TerminalReadPort.DashboardOverviewSnapshot(12500.25, 132.4, 0.61, 3));

        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEquity").value(12500.25))
                .andExpect(jsonPath("$.activeBots").value(3));
    }

    @Test
    void shouldGetDashboardEquitySeries() throws Exception {
        when(getDashboardEquitySeriesUseCase.execute("1M"))
                .thenReturn(List.of(
                        new TerminalReadPort.TimeSeriesPointSnapshot(LocalDateTime.of(2026, 4, 1, 10, 0), 100.5),
                        new TerminalReadPort.TimeSeriesPointSnapshot(LocalDateTime.of(2026, 4, 2, 10, 0), 101.5)
                ));

        mockMvc.perform(get("/api/v1/dashboard/equity-series").param("range", "1M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(100.5));
    }

    @Test
    void shouldGetDashboardExchangeAllocation() throws Exception {
        when(getDashboardExchangeAllocationUseCase.execute())
                .thenReturn(List.of(new TerminalReadPort.ExchangeAllocationSnapshot("BINANCE", 0.42)));

        mockMvc.perform(get("/api/v1/dashboard/exchange-allocation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exchange").value("BINANCE"));
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
}

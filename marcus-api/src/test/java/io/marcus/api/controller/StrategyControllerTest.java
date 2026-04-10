package io.marcus.api.controller;

import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.usecase.GetStrategyDetailUseCase;
import io.marcus.application.usecase.GetStrategyMetricsUseCase;
import io.marcus.application.usecase.GetStrategyPerformanceSeriesUseCase;
import io.marcus.application.usecase.ListStrategyTradesUseCase;
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

@WebMvcTest(StrategyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class StrategyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetStrategyDetailUseCase getStrategyDetailUseCase;

    @MockBean
    private GetStrategyMetricsUseCase getStrategyMetricsUseCase;

    @MockBean
    private GetStrategyPerformanceSeriesUseCase getStrategyPerformanceSeriesUseCase;

    @MockBean
    private ListStrategyTradesUseCase listStrategyTradesUseCase;

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
    void shouldGetStrategyDetail() throws Exception {
        when(getStrategyDetailUseCase.execute("stg_1"))
                .thenReturn(new TerminalReadPort.StrategyDetailSnapshot("stg_1", "Neutron", "Marcus", "CRYPTO", "ACTIVE"));

        mockMvc.perform(get("/api/v1/strategies/stg_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategyId").value("stg_1"));
    }

    @Test
    void shouldGetStrategyMetrics() throws Exception {
        when(getStrategyMetricsUseCase.execute("stg_1", "AFTER_FEES"))
                .thenReturn(new TerminalReadPort.StrategyMetricsSnapshot(0.2, 0.1, 1.4, 1.7, 1.1, 2.0));

        mockMvc.perform(get("/api/v1/strategies/stg_1/metrics").param("feeMode", "AFTER_FEES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharpe").value(1.4));
    }

    @Test
    void shouldGetStrategyPerformanceSeries() throws Exception {
        when(getStrategyPerformanceSeriesUseCase.execute("stg_1", "1M"))
                .thenReturn(List.of(
                        new TerminalReadPort.TimeSeriesPointSnapshot(LocalDateTime.of(2026, 4, 1, 10, 0), 100.0)
                ));

        mockMvc.perform(get("/api/v1/strategies/stg_1/performance-series").param("range", "1M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(100.0));
    }

    @Test
    void shouldGetStrategyTrades() throws Exception {
        TerminalReadPort.TradeLogPageSnapshot page = new TerminalReadPort.TradeLogPageSnapshot(
                List.of(
                        new TerminalReadPort.TradeLogSnapshot(LocalDateTime.of(2026, 4, 1, 10, 0), "BTCUSDT", "LONG", 1.2, 62000, 62500, 600)
                ),
                0,
                20,
                1L
        );
        when(listStrategyTradesUseCase.execute("stg_1", 0, 20, "BTCUSDT")).thenReturn(page);

        mockMvc.perform(get("/api/v1/strategies/stg_1/trades")
                        .param("page", "0")
                        .param("size", "20")
                        .param("asset", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].assetPair").value("BTCUSDT"));
    }

    @Test
    void shouldReturnBadRequestWhenFeeModeInvalid() throws Exception {
        when(getStrategyMetricsUseCase.execute("stg_1", "NET"))
                .thenThrow(new IllegalArgumentException("Unsupported fee mode: NET"));

        mockMvc.perform(get("/api/v1/strategies/stg_1/metrics").param("feeMode", "NET"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}

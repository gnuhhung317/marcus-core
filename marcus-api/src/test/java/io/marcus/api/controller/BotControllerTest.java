package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.dto.BacktestUploadRequest;
import io.marcus.application.dto.BacktestUploadResponse;
import io.marcus.application.dto.BotAnalyticsDtos;
import io.marcus.application.dto.BotDryRunSyncRequest;
import io.marcus.application.dto.BotTelemetryRequest;
import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.dto.UpdateBotStatusRequest;
import io.marcus.application.dto.UpdateBotMetadataRequest;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.usecase.GetBotDetailUseCase;
import io.marcus.application.usecase.GetBotIntegrationHealthUseCase;
import io.marcus.application.usecase.GetBotCredentialsUseCase;
import io.marcus.application.usecase.GetBotAnalyticsUseCase;
import io.marcus.application.usecase.GetLatestBotDryRunUseCase;
import io.marcus.application.usecase.GetLatestBotTelemetryUseCase;
import io.marcus.application.usecase.ListDeveloperBotsUseCase;
import io.marcus.application.usecase.ListBotTradesUseCase;
import io.marcus.application.usecase.ListPublicBotsUseCase;
import io.marcus.application.usecase.RegisterBotUseCase;
import io.marcus.application.usecase.SyncBotDryRunUseCase;
import io.marcus.application.usecase.SyncBotTelemetryUseCase;
import io.marcus.application.usecase.UploadBotBacktestResultUseCase;
import io.marcus.application.usecase.UpdateBotStatusUseCase;
import io.marcus.application.usecase.UpdateBotMetadataUseCase;
import io.marcus.application.usecase.DeleteBotUseCase;
import io.marcus.application.usecase.BotHeartbeatUseCase;
import io.marcus.domain.port.AccessTokenPort;
import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.port.UserProfileReadPort;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.BotDryRunClosedTrade;
import io.marcus.domain.model.BotDryRunPortfolioPoint;
import io.marcus.domain.model.BotDryRunPosition;
import io.marcus.domain.model.BotDryRunState;
import io.marcus.domain.model.BotTelemetryPoint;
import io.marcus.domain.vo.BotStatus;
import io.marcus.infrastructure.security.BotSignatureInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BotController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class BotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterBotUseCase registerBotUseCase;

    @MockBean
    private ListPublicBotsUseCase listPublicBotsUseCase;

    @MockBean
    private ListDeveloperBotsUseCase listDeveloperBotsUseCase;

    @MockBean
    private GetBotDetailUseCase getBotDetailUseCase;

    @MockBean
    private GetBotIntegrationHealthUseCase getBotIntegrationHealthUseCase;

    @MockBean
    private GetBotCredentialsUseCase getBotCredentialsUseCase;

    @MockBean
    private GetBotAnalyticsUseCase getBotAnalyticsUseCase;

    @MockBean
    private ListBotTradesUseCase listBotTradesUseCase;

    @MockBean
    private SyncBotTelemetryUseCase syncBotTelemetryUseCase;

    @MockBean
    private GetLatestBotTelemetryUseCase getLatestBotTelemetryUseCase;

    @MockBean
    private SyncBotDryRunUseCase syncBotDryRunUseCase;

    @MockBean
    private GetLatestBotDryRunUseCase getLatestBotDryRunUseCase;

    @MockBean
    private UploadBotBacktestResultUseCase uploadBotBacktestResultUseCase;

    @MockBean
    private UpdateBotStatusUseCase updateBotStatusUseCase;

    @MockBean
    private UpdateBotMetadataUseCase updateBotMetadataUseCase;

    @MockBean
    private DeleteBotUseCase deleteBotUseCase;

    @MockBean
    private BotHeartbeatUseCase botHeartbeatUseCase;

    @MockBean
    private AccessTokenPort accessTokenPort;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @BeforeEach
    void allowSignedBotRequestsThrough() throws Exception {
        when(botSignatureInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shouldReturnPublicBots() throws Exception {
        List<BotSummaryResult> response = List.of(
                BotSummaryResult.builder()
                        .botId("bot_001")
                        .botName("Public Bot")
                        .status("ACTIVE")
                        .tradingPair("BTCUSDT")
                        .exchange("binance")
                        .build()
        );

        when(listPublicBotsUseCase.execute(null, null, null, "-return", 0, 20)).thenReturn(
                new BotDiscoveryReadPort.BotDiscoveryPageSnapshot(
                        List.of(),
                        new UserProfileReadPort.OffsetPaginationMetaSnapshot(0, 20, 0, 0, false)
                )
        );

        mockMvc.perform(get("/api/v1/bots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(0));

        verify(listPublicBotsUseCase).execute(null, null, null, "-return", 0, 20);
    }

    @Test
    void shouldReturnMyBots() throws Exception {
        List<BotSummaryResult> response = List.of(
                BotSummaryResult.builder()
                        .botId("bot_123")
                        .botName("My Bot")
                        .status("ACTIVE")
                        .tradingPair("BTCUSDT")
                        .exchange("binance")
                        .apiKey("ak_123")
                        .build()
        );

        when(listDeveloperBotsUseCase.execute()).thenReturn(response);

        mockMvc.perform(get("/api/v1/bots/my-bots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].botId").value("bot_123"))
                .andExpect(jsonPath("$[0].apiKey").value("ak_123"));
    }

    @Test
    void shouldReturnCreatedWhenRegisterBotSuccessfully() throws Exception {
        RegisterBotRequest request = new RegisterBotRequest("Scalp strategy", "BTCUSDT", "Scalp Bot", "binance");
        BotRegistrationResult response = new BotRegistrationResult(
                "bot_123",
                "Scalp Bot",
                "Scalp strategy",
                "Scalp Bot",
                "ak_123",
                "sk_raw",
                "ACTIVE",
                "BTCUSDT",
                "binance"
        );

        when(registerBotUseCase.execute(any(RegisterBotRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.botId").value("bot_123"))
                .andExpect(jsonPath("$.apiKey").value("ak_123"));

        verify(registerBotUseCase).execute(request);
    }

    @Test
    void shouldReturnUnprocessableEntityWhenValidationFails() throws Exception {
        RegisterBotRequest request = new RegisterBotRequest(null, "BTCUSDT", "", "binance");

        mockMvc.perform(post("/api/v1/bots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(422));

        verifyNoInteractions(registerBotUseCase);
    }

    @Test
    void shouldReturnBadRequestWhenUseCaseThrowsIllegalArgument() throws Exception {
        RegisterBotRequest request = new RegisterBotRequest("Scalp strategy", "BTCUSDT", "Scalp Bot", "binance");
        when(registerBotUseCase.execute(any(RegisterBotRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid bot configuration"));

        mockMvc.perform(post("/api/v1/bots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid bot configuration"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldUpdateStatus() throws Exception {
        UpdateBotStatusRequest request = new UpdateBotStatusRequest(BotStatus.PAUSED);
        Bot response = Bot.builder().botId("bot_123").status(BotStatus.PAUSED).build();

        when(updateBotStatusUseCase.execute("bot_123", BotStatus.PAUSED)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/bots/bot_123/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botId").value("bot_123"))
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void shouldUpdateMetadata() throws Exception {
        UpdateBotMetadataRequest request = new UpdateBotMetadataRequest("New Name", "New Desc", "BTCUSDT", null, null, null);
        Bot response = Bot.builder().botId("bot_123").name("New Name").description("New Desc").tradingPair("BTCUSDT").build();

        when(updateBotMetadataUseCase.execute(eq("bot_123"), any(UpdateBotMetadataRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/bots/bot_123/metadata")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botId").value("bot_123"))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.description").value("New Desc"));
    }

    @Test
    void shouldDeleteBot() throws Exception {
        mockMvc.perform(delete("/api/v1/bots/bot_123"))
                .andExpect(status().isNoContent());

        verify(deleteBotUseCase).execute("bot_123");
    }

    @Test
    void shouldRegisterHeartbeat() throws Exception {
        mockMvc.perform(post("/api/v1/bots/bot_123/heartbeat")
                .header("X-Bot-Api-Key", "ak_123"))
                .andExpect(status().isOk());

        verify(botHeartbeatUseCase).execute("bot_123", "ak_123");
    }

    @Test
    void shouldReturnBotAnalyticsMetrics() throws Exception {
        BotAnalyticsDtos.MetricBlock block = new BotAnalyticsDtos.MetricBlock(0.2, -0.1, 1.5, 1.4, 2.0, 1.8, 0.6, 45, 12, null);
        when(getBotAnalyticsUseCase.getMetrics("bot_123"))
                .thenReturn(new BotAnalyticsDtos.GroupedMetricsResponse(block, block, block));

        mockMvc.perform(get("/api/v1/bots/bot_123/analytics/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.annualReturn").value(0.2))
                .andExpect(jsonPath("$.outOfSample.sharpe").value(1.5));
    }

    @Test
    void shouldReturnBotPerformanceSeries() throws Exception {
        LocalDateTime split = LocalDateTime.of(2026, 1, 2, 0, 0);
        when(getBotAnalyticsUseCase.getPerformanceSeries("bot_123", "ALL"))
                .thenReturn(new BotAnalyticsDtos.PerformanceSeriesResponse(
                        split,
                        List.of(new BotAnalyticsDtos.PerformancePoint(split, 12.3, "OUT_OF_SAMPLE"))
                ));

        mockMvc.perform(get("/api/v1/bots/bot_123/analytics/performance-series"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.splitTimestamp").exists())
                .andExpect(jsonPath("$.points[0].phase").value("OUT_OF_SAMPLE"));
    }

    @Test
    void shouldReturnBotTrades() throws Exception {
        BotDiscoveryReadPort.TradeLogPageSnapshot page = new BotDiscoveryReadPort.TradeLogPageSnapshot(
                List.of(new BotDiscoveryReadPort.TradeLogSnapshot(
                        LocalDateTime.of(2026, 1, 2, 12, 0),
                        "BTCUSDT",
                        "BUY",
                        0.5,
                        100.0,
                        110.0,
                        5.0
                )),
                0,
                20,
                1L
        );

        when(listBotTradesUseCase.execute("bot_123", 0, 20, "BTCUSDT")).thenReturn(page);

        mockMvc.perform(get("/api/v1/bots/bot_123/trades")
                .param("asset", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].assetPair").value("BTCUSDT"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldSyncBotTelemetry() throws Exception {
        BotTelemetryRequest request = new BotTelemetryRequest(
                LocalDateTime.of(2026, 1, 2, 0, 0),
                new BigDecimal("10100.50"),
                new BigDecimal("100.50"),
                new BigDecimal("25.00"),
                java.util.Map.of("latencyMs", 42)
        );
        when(syncBotTelemetryUseCase.execute(eq("bot_123"), eq("ak_123"), any(BotTelemetryRequest.class)))
                .thenReturn(new BotTelemetryPoint("bot_123", request.timestamp(), request.equity(), request.realizedPnl(), request.unrealizedPnl(), "{\"latencyMs\":42}"));

        mockMvc.perform(post("/api/v1/bots/bot_123/telemetry")
                .header("X-Bot-Api-Key", "ak_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equity").value(10100.50));
    }

    @Test
    void shouldUploadBotBacktestResults() throws Exception {
        LocalDateTime startedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 1, 2, 0, 0);
        BacktestUploadRequest request = new BacktestUploadRequest(
                "baseline",
                startedAt,
                endedAt,
                java.util.Map.of("total_return", 0.12),
                List.of(new BacktestUploadRequest.EquityPoint(
                        startedAt,
                        new BigDecimal("10000"),
                        new BigDecimal("10000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )),
                List.of()
        );
        when(uploadBotBacktestResultUseCase.execute(eq("bot_123"), eq("ak_123"), any(BacktestUploadRequest.class)))
                .thenReturn(new BacktestUploadResponse("bt_1", "bot_123", "baseline", 1, 0, startedAt, endedAt));

        mockMvc.perform(post("/api/v1/bots/bot_123/backtest-results")
                .header("X-Bot-Api-Key", "ak_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value("bt_1"))
                .andExpect(jsonPath("$.equityPoints").value(1));
    }

    @Test
    void shouldUploadBotBacktestResultsWithOffsetTimestamps() throws Exception {
        String jsonPayload = """
                {
                  "runName": "baseline-offset",
                  "startedAt": "2026-03-24T05:00:00+00:00",
                  "endedAt": "2026-03-24T06:00:00+00:00",
                  "metrics": {"total_return": 0.12},
                  "equityHistory": [
                    {
                      "timestamp": "2026-03-24T05:00:00+00:00",
                      "cash": 10000,
                      "equity": 10000,
                      "realizedPnl": 0,
                      "unrealizedPnl": 0,
                      "totalFees": 0
                    }
                  ],
                  "closedTrades": [
                    {
                      "symbol": "BTCUSDT",
                      "marketType": "SPOT",
                      "side": "BUY",
                      "quantity": 1,
                      "entryPrice": 60000,
                      "exitPrice": 61000,
                      "pnl": 1000,
                      "fees": 10,
                      "entryTimestamp": "2026-03-24T05:00:00+00:00",
                      "exitTimestamp": "2026-03-24T05:30:00+00:00",
                      "durationSeconds": 1800
                    }
                  ]
                }
                """;

        LocalDateTime startedAt = LocalDateTime.of(2026, 3, 24, 5, 0, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 3, 24, 6, 0, 0);
        when(uploadBotBacktestResultUseCase.execute(eq("bot_123"), eq("ak_123"), any(BacktestUploadRequest.class)))
                .thenReturn(new BacktestUploadResponse("bt_1", "bot_123", "baseline-offset", 1, 1, startedAt, endedAt));

        mockMvc.perform(post("/api/v1/bots/bot_123/backtest-results")
                .header("X-Bot-Api-Key", "ak_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value("bt_1"))
                .andExpect(jsonPath("$.closedTrades").value(1));
    }

    @Test
    void shouldSyncBotDryRunState() throws Exception {
        LocalDateTime snapshotAt = LocalDateTime.of(2026, 1, 2, 0, 0);
        BotDryRunSyncRequest request = new BotDryRunSyncRequest(
                new BotDryRunSyncRequest.Portfolio(
                        snapshotAt,
                        new BigDecimal("10000.00"),
                        new BigDecimal("10100.50"),
                        new BigDecimal("100.50"),
                        new BigDecimal("25.00"),
                        new BigDecimal("2.50")
                ),
                List.of(new BotDryRunSyncRequest.Position(
                        "SPOT:BTCUSDT",
                        "BTCUSDT",
                        "SPOT",
                        "LONG",
                        new BigDecimal("0.1"),
                        new BigDecimal("65000"),
                        new BigDecimal("66200"),
                        new BigDecimal("120"),
                        snapshotAt.minusHours(1),
                        "sig_123"
                )),
                List.of()
        );
        when(syncBotDryRunUseCase.execute(eq("bot_123"), eq("ak_123"), any(BotDryRunSyncRequest.class)))
                .thenReturn(new BotDryRunState(
                        new BotDryRunPortfolioPoint("bot_123", snapshotAt,
                                new BigDecimal("10000.00"),
                                new BigDecimal("10100.50"),
                                new BigDecimal("100.50"),
                                new BigDecimal("25.00"),
                                new BigDecimal("2.50")),
                        List.of(new BotDryRunPosition("bot_123", "SPOT:BTCUSDT", "BTCUSDT", "SPOT", "LONG",
                                new BigDecimal("0.1"), new BigDecimal("65000"), new BigDecimal("66200"), new BigDecimal("120"),
                                snapshotAt.minusHours(1), "sig_123", "OPEN")),
                        List.of()
                ));

        mockMvc.perform(post("/api/v1/bots/bot_123/dry-run/sync")
                .header("X-Bot-Api-Key", "ak_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolio.equity").value(10100.50))
                .andExpect(jsonPath("$.positions[0].positionId").value("SPOT:BTCUSDT"));
    }

    @Test
    void shouldReturnLatestBotDryRunState() throws Exception {
        LocalDateTime snapshotAt = LocalDateTime.of(2026, 1, 2, 0, 0);
        when(getLatestBotDryRunUseCase.execute("bot_123", "ak_123"))
                .thenReturn(new BotDryRunState(
                        new BotDryRunPortfolioPoint(
                                "bot_123",
                                snapshotAt,
                                new BigDecimal("10000.00"),
                                new BigDecimal("10100.50"),
                                new BigDecimal("100.50"),
                                new BigDecimal("25.00"),
                                new BigDecimal("2.50")
                        ),
                        List.of(),
                        List.of(new BotDryRunClosedTrade("bot_123", "trade_1", "BTCUSDT", "SPOT", "LONG",
                                new BigDecimal("0.1"), new BigDecimal("65000"), new BigDecimal("66800"),
                                new BigDecimal("180"), new BigDecimal("2.5"),
                                snapshotAt.minusHours(1), snapshotAt, "sig_123", "sig_456"))
                ));

        mockMvc.perform(get("/api/v1/bots/bot_123/dry-run/latest")
                .header("X-Bot-Api-Key", "ak_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedTrades[0].tradeId").value("trade_1"));
    }

    @Test
    void shouldReturnLatestBotTelemetry() throws Exception {
        when(getLatestBotTelemetryUseCase.execute("bot_123", "ak_123"))
                .thenReturn(new BotAnalyticsDtos.TelemetrySnapshot(
                        LocalDateTime.of(2026, 1, 2, 0, 0),
                        new BigDecimal("10100.50"),
                        new BigDecimal("100.50"),
                        new BigDecimal("25.00"),
                        "{\"latencyMs\":42}"
                ));

        mockMvc.perform(get("/api/v1/bots/bot_123/telemetry/latest")
                .header("X-Bot-Api-Key", "ak_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realizedPnl").value(100.50));
    }
}

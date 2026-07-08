package io.marcus.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.dto.BacktestUploadRequest;
import io.marcus.application.dto.BacktestUploadResponse;
import io.marcus.application.dto.BotAnalyticsDtos;
import io.marcus.application.dto.BotDryRunSyncRequest;
import io.marcus.application.dto.BotTelemetryRequest;
import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.dto.UpdateBotStatusRequest;
import io.marcus.application.dto.UpdateBotMetadataRequest;
import io.marcus.application.usecase.GetBotAnalyticsUseCase;
import io.marcus.application.usecase.GetBotDetailUseCase;
import io.marcus.application.usecase.ListDeveloperBotsUseCase;
import io.marcus.application.usecase.ListBotTradesUseCase;
import io.marcus.application.usecase.ListPublicBotsUseCase;
import io.marcus.application.usecase.RegisterBotUseCase;
import io.marcus.application.usecase.GetBotIntegrationHealthUseCase;
import io.marcus.application.usecase.GetBotCredentialsUseCase;
import io.marcus.application.usecase.GetLatestBotTelemetryUseCase;
import io.marcus.application.usecase.GetLatestBotDryRunUseCase;
import io.marcus.application.usecase.SyncBotTelemetryUseCase;
import io.marcus.application.usecase.SyncBotDryRunUseCase;
import io.marcus.application.usecase.UploadBotBacktestResultUseCase;
import io.marcus.application.usecase.UpdateBotStatusUseCase;
import io.marcus.application.usecase.UpdateBotMetadataUseCase;
import io.marcus.application.usecase.DeleteBotUseCase;
import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.BotDryRunState;
import io.marcus.application.usecase.BotHeartbeatUseCase;
import io.marcus.infrastructure.cache.RedisCacheFacade;
import io.marcus.infrastructure.cache.RedisCacheInvalidator;
import io.marcus.infrastructure.security.RequireBotSignature;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping({"/bots", "/api/bots", "/api/v1/bots"})
@RequiredArgsConstructor
public class BotController {

    private static final Duration BOT_ANALYTICS_TTL = Duration.ofSeconds(30);
    private static final TypeReference<BotAnalyticsDtos.GroupedMetricsResponse> BOT_ANALYTICS_METRICS_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<BotAnalyticsDtos.PerformanceSeriesResponse> BOT_ANALYTICS_SERIES_TYPE =
            new TypeReference<>() {};

    private final RegisterBotUseCase registerBotUseCase;
    private final ListPublicBotsUseCase listPublicBotsUseCase;
    private final ListDeveloperBotsUseCase listDeveloperBotsUseCase;
    private final GetBotDetailUseCase getBotDetailUseCase;
    private final GetBotIntegrationHealthUseCase getBotIntegrationHealthUseCase;
    private final GetBotCredentialsUseCase getBotCredentialsUseCase;
    private final GetBotAnalyticsUseCase getBotAnalyticsUseCase;
    private final SyncBotTelemetryUseCase syncBotTelemetryUseCase;
    private final GetLatestBotTelemetryUseCase getLatestBotTelemetryUseCase;
    private final SyncBotDryRunUseCase syncBotDryRunUseCase;
    private final GetLatestBotDryRunUseCase getLatestBotDryRunUseCase;
    private final UploadBotBacktestResultUseCase uploadBotBacktestResultUseCase;
    private final UpdateBotStatusUseCase updateBotStatusUseCase;
    private final UpdateBotMetadataUseCase updateBotMetadataUseCase;
    private final DeleteBotUseCase deleteBotUseCase;
    private final BotHeartbeatUseCase botHeartbeatUseCase;
    private final ListBotTradesUseCase listBotTradesUseCase;

    @Autowired(required = false)
    private RedisCacheFacade cacheFacade;

    @Autowired(required = false)
    private RedisCacheInvalidator cacheInvalidator;

    @PostMapping({"", "/register"})
    public ResponseEntity<BotRegistrationResult> registerBot(@Valid @RequestBody RegisterBotRequest botRequest) {
        BotRegistrationResult result = registerBotUseCase.execute(botRequest);
        evictBotCatalog(result.botId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(result);
    }

    @GetMapping
    public ResponseEntity<BotDiscoveryReadPort.BotDiscoveryPageSnapshot> listPublicBots(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String asset,
            @RequestParam(required = false) String risk,
            @RequestParam(required = false, defaultValue = "-return") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(listPublicBotsUseCase.execute(q, asset, risk, sort, page, size));
    }

    @GetMapping("/my-bots")
    public ResponseEntity<List<BotSummaryResult>> getMyBots() {
        return ResponseEntity.ok(listDeveloperBotsUseCase.execute());
    }

    @GetMapping("/{botId}")
    public ResponseEntity<BotDiscoveryReadPort.BotDetailSnapshot> getBotDetail(
            @PathVariable String botId,
            @RequestParam(required = false, defaultValue = "AUTO") String source
    ) {
        return ResponseEntity.ok(getBotDetailUseCase.execute(botId, source));
    }

    @GetMapping("/{botId}/integration-health")
    public ResponseEntity<PortfolioReadPort.BotIntegrationHealthSnapshot> getBotIntegrationHealth(@PathVariable String botId) {
        return ResponseEntity.ok(getBotIntegrationHealthUseCase.execute(botId));
    }

    @GetMapping("/{botId}/credentials")
    public ResponseEntity<PortfolioReadPort.ApiKeySnapshot> getBotCredentials(@PathVariable String botId) {
        return ResponseEntity.ok(getBotCredentialsUseCase.execute(botId));
    }

    @GetMapping("/{botId}/analytics/metrics")
    public ResponseEntity<BotAnalyticsDtos.GroupedMetricsResponse> getBotAnalyticsMetrics(@PathVariable String botId) {
        return ResponseEntity.ok(cacheOrLoad(
                "bot-analytics:metrics:" + RedisCacheFacade.keyPart(botId),
                BOT_ANALYTICS_TTL,
                BOT_ANALYTICS_METRICS_TYPE,
                () -> getBotAnalyticsUseCase.getMetrics(botId)
        ));
    }

    @GetMapping("/{botId}/analytics/performance-series")
    public ResponseEntity<BotAnalyticsDtos.PerformanceSeriesResponse> getBotPerformanceSeries(
            @PathVariable String botId,
            @RequestParam(required = false, defaultValue = "ALL") String range
    ) {
        return ResponseEntity.ok(cacheOrLoad(
                "bot-analytics:series:%s:%s".formatted(
                        RedisCacheFacade.keyPart(botId),
                        RedisCacheFacade.keyPart(range)
                ),
                BOT_ANALYTICS_TTL,
                BOT_ANALYTICS_SERIES_TYPE,
                () -> getBotAnalyticsUseCase.getPerformanceSeries(botId, range)
        ));
    }

    @GetMapping("/{botId}/trades")
    public ResponseEntity<BotDiscoveryReadPort.TradeLogPageSnapshot> listBotTrades(
            @PathVariable String botId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String asset
    ) {
        return ResponseEntity.ok(listBotTradesUseCase.execute(botId, page, size, asset));
    }

    @PostMapping("/{botId}/backtest-results")
    @RequireBotSignature
    public ResponseEntity<BacktestUploadResponse> uploadBacktestResults(
            @PathVariable String botId,
            @RequestHeader("X-Bot-Api-Key") String apiKey,
            @Valid @RequestBody BacktestUploadRequest request
    ) {
        BacktestUploadResponse response = uploadBotBacktestResultUseCase.execute(botId, apiKey, request);
        evictBotAnalyticsAndCatalog(botId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{botId}/dry-run/sync")
    @RequireBotSignature
    public ResponseEntity<BotAnalyticsDtos.DryRunStateResponse> syncDryRun(
            @PathVariable String botId,
            @RequestHeader("X-Bot-Api-Key") String apiKey,
            @Valid @RequestBody BotDryRunSyncRequest request
    ) {
        BotAnalyticsDtos.DryRunStateResponse response = toDryRunResponse(syncBotDryRunUseCase.execute(botId, apiKey, request));
        evictBotAnalyticsAndCatalog(botId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{botId}/dry-run/latest")
    @RequireBotSignature
    public ResponseEntity<BotAnalyticsDtos.DryRunStateResponse> latestDryRun(
            @PathVariable String botId,
            @RequestHeader("X-Bot-Api-Key") String apiKey
    ) {
        BotDryRunState snapshot = getLatestBotDryRunUseCase.execute(botId, apiKey);
        if (snapshot == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(toDryRunResponse(snapshot));
    }

    @PostMapping("/{botId}/telemetry")
    @RequireBotSignature
    public ResponseEntity<BotAnalyticsDtos.TelemetrySnapshot> syncTelemetry(
            @PathVariable String botId,
            @RequestHeader("X-Bot-Api-Key") String apiKey,
            @Valid @RequestBody BotTelemetryRequest request
    ) {
        var point = syncBotTelemetryUseCase.execute(botId, apiKey, request);
        return ResponseEntity.ok(new BotAnalyticsDtos.TelemetrySnapshot(
                point.timestamp(),
                point.equity(),
                point.realizedPnl(),
                point.unrealizedPnl(),
                point.metricsJson()
        ));
    }

    @GetMapping("/{botId}/telemetry/latest")
    @RequireBotSignature
    public ResponseEntity<BotAnalyticsDtos.TelemetrySnapshot> latestTelemetry(
            @PathVariable String botId,
            @RequestHeader("X-Bot-Api-Key") String apiKey
    ) {
        BotAnalyticsDtos.TelemetrySnapshot snapshot = getLatestBotTelemetryUseCase.execute(botId, apiKey);
        if (snapshot == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(snapshot);
    }

    @PatchMapping("/{botId}/status")
    public ResponseEntity<Bot> updateBotStatus(
            @PathVariable String botId,
            @RequestBody UpdateBotStatusRequest request
    ) {
        Bot updated = updateBotStatusUseCase.execute(botId, request.status());
        evictBotCatalog(botId);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{botId}/metadata")
    public ResponseEntity<Bot> updateBotMetadata(
            @PathVariable String botId,
            @RequestBody UpdateBotMetadataRequest request
    ) {
        Bot updated = updateBotMetadataUseCase.execute(botId, request);
        evictBotCatalog(botId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{botId}")
    public ResponseEntity<Void> deleteBot(@PathVariable String botId) {
        deleteBotUseCase.execute(botId);
        evictBotCatalog(botId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{botId}/heartbeat")
    @RequireBotSignature
    public ResponseEntity<Void> botHeartbeat(
            @PathVariable String botId,
            @RequestHeader("X-Bot-Api-Key") String apiKey
    ) {
        botHeartbeatUseCase.execute(botId, apiKey);
        return ResponseEntity.ok().build();
    }

    private <T> T cacheOrLoad(String key, Duration ttl, TypeReference<T> typeReference, Supplier<T> loader) {
        if (cacheFacade == null) {
            return loader.get();
        }
        return cacheFacade.getOrLoad(key, ttl, typeReference, loader);
    }

    private void evictBotCatalog(String botId) {
        if (cacheInvalidator != null) {
            cacheInvalidator.evictBotCatalog(botId);
        }
    }

    private void evictBotAnalyticsAndCatalog(String botId) {
        if (cacheInvalidator != null) {
            cacheInvalidator.evictBotAnalyticsAndCatalog(botId);
        }
    }

    private BotAnalyticsDtos.DryRunStateResponse toDryRunResponse(BotDryRunState state) {
        return new BotAnalyticsDtos.DryRunStateResponse(
                new BotAnalyticsDtos.DryRunPortfolioSnapshot(
                        state.portfolio().timestamp(),
                        state.portfolio().cash(),
                        state.portfolio().equity(),
                        state.portfolio().realizedPnl(),
                        state.portfolio().unrealizedPnl(),
                        state.portfolio().totalFees()
                ),
                state.positions().stream()
                        .map(position -> new BotAnalyticsDtos.DryRunPositionSnapshot(
                                position.positionId(),
                                position.symbol(),
                                position.marketType(),
                                position.side(),
                                position.quantity(),
                                position.entryPrice(),
                                position.currentPrice(),
                                position.unrealizedPnl(),
                                position.openedAt(),
                                position.sourceSignalId(),
                                position.status()
                        ))
                        .toList(),
                state.closedTrades().stream()
                        .map(trade -> new BotAnalyticsDtos.DryRunClosedTradeSnapshot(
                                trade.tradeId(),
                                trade.symbol(),
                                trade.marketType(),
                                trade.side(),
                                trade.quantity(),
                                trade.entryPrice(),
                                trade.exitPrice(),
                                trade.pnl(),
                                trade.fees(),
                                trade.entryTimestamp(),
                                trade.exitTimestamp(),
                                trade.entrySignalId(),
                                trade.exitSignalId()
                        ))
                        .toList()
        );
    }
}

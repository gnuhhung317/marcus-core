package io.marcus.api.controller;

import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.dto.BotAnalyticsDtos;
import io.marcus.application.dto.BotTelemetryRequest;
import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.dto.UpdateBotStatusRequest;
import io.marcus.application.dto.UpdateBotMetadataRequest;
import io.marcus.application.usecase.GetBotAnalyticsUseCase;
import io.marcus.application.usecase.GetBotDetailUseCase;
import io.marcus.application.usecase.ListDeveloperBotsUseCase;
import io.marcus.application.usecase.ListPublicBotsUseCase;
import io.marcus.application.usecase.RegisterBotUseCase;
import io.marcus.application.usecase.GetBotIntegrationHealthUseCase;
import io.marcus.application.usecase.GetBotCredentialsUseCase;
import io.marcus.application.usecase.GetLatestBotTelemetryUseCase;
import io.marcus.application.usecase.SyncBotTelemetryUseCase;
import io.marcus.application.usecase.UpdateBotStatusUseCase;
import io.marcus.application.usecase.UpdateBotMetadataUseCase;
import io.marcus.application.usecase.DeleteBotUseCase;
import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.model.Bot;
import io.marcus.application.usecase.BotHeartbeatUseCase;
import io.marcus.infrastructure.security.RequireBotSignature;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping({"/bots", "/api/bots", "/api/v1/bots"})
@RequiredArgsConstructor
public class BotController {

    private final RegisterBotUseCase registerBotUseCase;
    private final ListPublicBotsUseCase listPublicBotsUseCase;
    private final ListDeveloperBotsUseCase listDeveloperBotsUseCase;
    private final GetBotDetailUseCase getBotDetailUseCase;
    private final GetBotIntegrationHealthUseCase getBotIntegrationHealthUseCase;
    private final GetBotCredentialsUseCase getBotCredentialsUseCase;
    private final GetBotAnalyticsUseCase getBotAnalyticsUseCase;
    private final SyncBotTelemetryUseCase syncBotTelemetryUseCase;
    private final GetLatestBotTelemetryUseCase getLatestBotTelemetryUseCase;
    private final UpdateBotStatusUseCase updateBotStatusUseCase;
    private final UpdateBotMetadataUseCase updateBotMetadataUseCase;
    private final DeleteBotUseCase deleteBotUseCase;
    private final BotHeartbeatUseCase botHeartbeatUseCase;

    @PostMapping({"", "/register"})
    public ResponseEntity<BotRegistrationResult> registerBot(@Valid @RequestBody RegisterBotRequest botRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registerBotUseCase.execute(botRequest));
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
            @PathVariable String botId
    ) {
        return ResponseEntity.ok(getBotDetailUseCase.execute(botId));
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
        return ResponseEntity.ok(getBotAnalyticsUseCase.getMetrics(botId));
    }

    @GetMapping("/{botId}/analytics/performance-series")
    public ResponseEntity<BotAnalyticsDtos.PerformanceSeriesResponse> getBotPerformanceSeries(
            @PathVariable String botId,
            @RequestParam(required = false, defaultValue = "ALL") String range
    ) {
        return ResponseEntity.ok(getBotAnalyticsUseCase.getPerformanceSeries(botId, range));
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
                point.unrealizedPnl()
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
        return ResponseEntity.ok(updateBotStatusUseCase.execute(botId, request.status()));
    }

    @PatchMapping("/{botId}/metadata")
    public ResponseEntity<Bot> updateBotMetadata(
            @PathVariable String botId,
            @RequestBody UpdateBotMetadataRequest request
    ) {
        return ResponseEntity.ok(updateBotMetadataUseCase.execute(botId, request));
    }

    @DeleteMapping("/{botId}")
    public ResponseEntity<Void> deleteBot(@PathVariable String botId) {
        deleteBotUseCase.execute(botId);
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
}

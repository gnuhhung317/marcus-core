package io.marcus.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.application.dto.BacktestUploadRequest;
import io.marcus.application.dto.BacktestUploadResponse;
import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.BotBacktestRun;
import io.marcus.domain.model.BotDryRunPortfolioPoint;
import io.marcus.domain.model.BotHistoricalClosedTrade;
import io.marcus.domain.port.BotBacktestPort;
import io.marcus.domain.port.LeaderboardMetricsRefreshPort;
import io.marcus.domain.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadBotBacktestResultUseCase {

    private final BotRepository botRepository;
    private final BotBacktestPort botBacktestPort;
    private final LeaderboardMetricsRefreshPort leaderboardMetricsRefreshPort;
    private final ObjectMapper objectMapper;

    public BacktestUploadResponse execute(String botId, String apiKey, BacktestUploadRequest request) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId is required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("backtest request is required");
        }

        String normalizedBotId = botId.trim();
        Bot bot = botRepository.findByBotId(normalizedBotId)
                .orElseThrow(() -> new BotNotFoundException("Bot not found: " + normalizedBotId));
        if (!apiKey.trim().equals(bot.getApiKey())) {
            throw new IllegalArgumentException("API Key mismatch for bot: " + normalizedBotId);
        }

        String runId = "bt_" + UUID.randomUUID();
        LocalDateTime startedAt = request.startedAt() != null ? request.startedAt() : request.equityHistory().get(0).timestamp();
        LocalDateTime endedAt = request.endedAt() != null ? request.endedAt() : request.equityHistory().get(request.equityHistory().size() - 1).timestamp();
        BotBacktestRun run = new BotBacktestRun(
                runId,
                normalizedBotId,
                request.runName(),
                startedAt,
                endedAt,
                metricsJson(request),
                LocalDateTime.now()
        );

        List<BotDryRunPortfolioPoint> equityHistory = request.equityHistory().stream()
                .map(point -> new BotDryRunPortfolioPoint(
                        normalizedBotId,
                        point.timestamp(),
                        point.cash(),
                        point.equity(),
                        point.realizedPnl(),
                        point.unrealizedPnl(),
                        point.totalFees()
                ))
                .toList();

        List<BotHistoricalClosedTrade> closedTrades = IntStream.range(0, request.closedTrades().size())
                .mapToObj(index -> toClosedTrade(runId, normalizedBotId, index, request.closedTrades().get(index)))
                .toList();

        BotBacktestRun saved = botBacktestPort.saveRun(run, equityHistory, closedTrades);
        try {
            leaderboardMetricsRefreshPort.recalculateForBot(normalizedBotId);
        } catch (Exception ex) {
            log.warn("Failed to refresh leaderboard metrics after backtest upload for bot {}", normalizedBotId, ex);
        }
        return new BacktestUploadResponse(
                saved.runId(),
                saved.botId(),
                saved.runName(),
                equityHistory.size(),
                closedTrades.size(),
                saved.startedAt(),
                saved.endedAt()
        );
    }

    private BotHistoricalClosedTrade toClosedTrade(String runId, String botId, int index, BacktestUploadRequest.ClosedTrade trade) {
        return new BotHistoricalClosedTrade(
                runId,
                botId,
                "bt_trade_" + index,
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
                trade.durationSeconds() != null ? trade.durationSeconds() : BigDecimal.ZERO
        );
    }

    private String metricsJson(BacktestUploadRequest request) {
        try {
            return objectMapper.writeValueAsString(request.metrics() == null ? java.util.Map.of() : request.metrics());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid backtest metrics payload", ex);
        }
    }
}

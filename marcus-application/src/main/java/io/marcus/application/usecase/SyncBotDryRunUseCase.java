package io.marcus.application.usecase;

import io.marcus.application.dto.BotDryRunSyncRequest;
import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.BotDryRunClosedTrade;
import io.marcus.domain.model.BotDryRunPortfolioPoint;
import io.marcus.domain.model.BotDryRunPosition;
import io.marcus.domain.model.BotDryRunState;
import io.marcus.domain.port.BotDryRunPort;
import io.marcus.domain.port.LeaderboardMetricsRefreshPort;
import io.marcus.domain.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncBotDryRunUseCase {

    private final BotRepository botRepository;
    private final BotDryRunPort botDryRunPort;
    private final LeaderboardMetricsRefreshPort leaderboardMetricsRefreshPort;

    public BotDryRunState execute(String botId, String apiKey, BotDryRunSyncRequest request) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId is required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("dry-run request is required");
        }

        String normalizedBotId = botId.trim();
        Bot bot = botRepository.findByBotId(normalizedBotId)
                .orElseThrow(() -> new BotNotFoundException("Bot not found: " + normalizedBotId));
        if (!apiKey.trim().equals(bot.getApiKey())) {
            throw new IllegalArgumentException("API Key mismatch for bot: " + normalizedBotId);
        }

        BotDryRunState state = new BotDryRunState(
                new BotDryRunPortfolioPoint(
                        normalizedBotId,
                        request.portfolio().timestamp(),
                        request.portfolio().cash(),
                        request.portfolio().equity(),
                        request.portfolio().realizedPnl(),
                        request.portfolio().unrealizedPnl(),
                        request.portfolio().totalFees()
                ),
                request.positions().stream()
                        .map(position -> new BotDryRunPosition(
                                normalizedBotId,
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
                                "OPEN"
                        ))
                        .toList(),
                request.closedTrades().stream()
                        .map(trade -> new BotDryRunClosedTrade(
                                normalizedBotId,
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

        BotDryRunState saved = botDryRunPort.syncSnapshot(state);
        try {
            leaderboardMetricsRefreshPort.recalculateForBot(normalizedBotId);
        } catch (Exception ex) {
            log.warn("Failed to refresh leaderboard metrics after dry-run sync for bot {}", normalizedBotId, ex);
        }
        return saved;
    }
}

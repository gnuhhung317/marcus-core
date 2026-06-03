package io.marcus.domain.port;

import io.marcus.domain.port.UserProfileReadPort.OffsetPaginationMetaSnapshot;
import io.marcus.domain.port.PortfolioReadPort.TimeSeriesPointSnapshot;
import java.time.LocalDateTime;
import java.util.List;

public interface BotDiscoveryReadPort {

    record BotDetailSnapshot(
            String botId,
            String botName,
            String description,
            String status,
            String tradingPair,
            String exchange,
            String developerId,
            String apiKey,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            BotPerformanceSnapshot performance
            ) {

    }

    record BotPerformanceSnapshot(
            double annualReturn,
            double maxDrawdown,
            double sharpe,
            double winRate,
            double avgTradeReturn,
            double tradesPerDay
            ) {

    }

    record BotDiscoverySnapshot(
            String botId,
            String botName,
            String description,
            String asset,
            String risk,
            double annualReturn,
            double maxDrawdown,
            int subscribers
            ) {

    }

    record BotDiscoveryPageSnapshot(
            List<BotDiscoverySnapshot> items,
            OffsetPaginationMetaSnapshot meta
            ) {

    }

    record FavoriteStrategySnapshot(
            String strategyId,
            boolean favorited
            ) {

    }

    record StrategyDetailSnapshot(
            String strategyId,
            String strategyName,
            String ownerName,
            String market,
            String status
            ) {

    }

    record StrategyMetricsSnapshot(
            double annualReturn,
            double maxDrawdown,
            double sharpe,
            double sortino,
            double calmar,
            double profitFactor
            ) {

    }

    record TradeLogSnapshot(
            LocalDateTime timestamp,
            String assetPair,
            String side,
            double size,
            double entryPrice,
            double exitPrice,
            double netPnl
            ) {

    }

    record TradeLogPageSnapshot(List<TradeLogSnapshot> items, int page, int size, long totalElements) {

    }

    record LeaderboardStrategySnapshot(
            int rank,
            String strategyId,
            String strategyName,
            String creatorName,
            double cagr,
            double sharpe,
            double maxDrawdown,
            String dataSource // "DRY_RUN" or "HISTORICAL"
            ) {

    }

    record LeaderboardStrategiesPageSnapshot(
            List<LeaderboardStrategySnapshot> items,
            OffsetPaginationMetaSnapshot meta
            ) {

    }

    record LeaderboardFeaturedItemSnapshot(
            String strategyId,
            String strategyName,
            String rankLabel,
            double sharpe
            ) {

    }

    record LeaderboardFeaturedSnapshot(List<LeaderboardFeaturedItemSnapshot> items) {

    }

    record StrategySpotlightSnapshot(
            String strategyId,
            String strategyName,
            String market,
            double oneDayReturn
            ) {

    }

    BotDetailSnapshot getBotDetail(String botId);

    BotDiscoveryPageSnapshot listPublicBots(String q, String asset, String risk, String sort, int page, int size);

    FavoriteStrategySnapshot favoriteStrategy(String userId, String strategyId);

    StrategyDetailSnapshot getStrategyDetail(String strategyId);

    StrategyMetricsSnapshot getStrategyMetrics(String strategyId, String feeMode);

    List<TimeSeriesPointSnapshot> listStrategyPerformanceSeries(String strategyId, String range);

    TradeLogPageSnapshot listStrategyTrades(String strategyId, int page, int size, String asset);

    LeaderboardStrategiesPageSnapshot listLeaderboardStrategies(
            String timeframe,
            String market,
            String asset,
            String rankMetric,
            int page,
            int size
    );

    LeaderboardFeaturedSnapshot listLeaderboardFeatured();

    List<StrategySpotlightSnapshot> listLeaderboardSpotlights();
}

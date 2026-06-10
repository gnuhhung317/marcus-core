package io.marcus.domain.port;

import io.marcus.domain.port.UserProfileReadPort.OffsetPaginationMetaSnapshot;
import io.marcus.domain.vo.LeaderboardDataSource;
import io.marcus.domain.vo.LeaderboardRankMetric;
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
            double winRate,
            int subscribers
            ) {

    }

    record BotDiscoveryPageSnapshot(
            List<BotDiscoverySnapshot> items,
            OffsetPaginationMetaSnapshot meta
            ) {

    }

    record FavoriteBotSnapshot(
            String botId,
            boolean favorited
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

    record LeaderboardBotSnapshot(
            int rank,
            String botId,
            String botName,
            String creatorName,
            double cagr,
            double sharpe,
            double maxDrawdown,
            String dataSource // "DRY_RUN" or "HISTORICAL"
            ) {

    }

    record LeaderboardBotsPageSnapshot(
            List<LeaderboardBotSnapshot> items,
            OffsetPaginationMetaSnapshot meta
            ) {

    }

    record LeaderboardFeaturedItemSnapshot(
            String botId,
            String botName,
            String rankLabel,
            double sharpe
            ) {

    }

    record LeaderboardFeaturedSnapshot(List<LeaderboardFeaturedItemSnapshot> items) {

    }

    record BotSpotlightSnapshot(
            String botId,
            String botName,
            String market,
            double oneDayReturn
            ) {

    }

    BotDetailSnapshot getBotDetail(String botId);

    BotDiscoveryPageSnapshot listPublicBots(String q, String asset, String risk, String sort, int page, int size);

    FavoriteBotSnapshot favoriteBot(String userId, String botId);

    TradeLogPageSnapshot listBotTrades(String botId, int page, int size, String asset);

    LeaderboardBotsPageSnapshot listLeaderboardBots(
            LeaderboardDataSource dataSource,
            String market,
            String asset,
            LeaderboardRankMetric rankMetric,
            int page,
            int size
    );

    LeaderboardFeaturedSnapshot listLeaderboardFeatured();

    List<BotSpotlightSnapshot> listLeaderboardSpotlights();
}

package io.marcus.domain.port;

import java.time.LocalDateTime;
import java.util.List;

public interface TerminalReadPort {

    BotDetailSnapshot getBotDetail(String botId);

    BotDiscoveryPageSnapshot listPublicBots(String q, String asset, String risk, String sort, int page, int size);

    FavoriteStrategySnapshot favoriteStrategy(String userId, String strategyId);

    DashboardOverviewSnapshot getDashboardOverview(String userId);

    List<TimeSeriesPointSnapshot> listDashboardEquitySeries(String userId, String range);

    List<ExchangeAllocationSnapshot> listExchangeAllocation(String userId);

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

    PaperSessionSummarySnapshot getPaperSessionSummary(String userId);

    List<PaperSignalSnapshot> listPaperSignals(String status, int limit);

    PaperExecutionLogPageSnapshot listPaperExecutionLogs(String userId, String cursor, int limit);

    PaperOrderSnapshot createPaperOrder(String userId, PaperOrderCreateSnapshot request);

    PaperSessionStateSnapshot pausePaperSession(String userId);

    PaperSessionStateSnapshot resumePaperSession(String userId);

    UserProfileSnapshot getCurrentUserProfile(String userId);

    UserPreferencesSnapshot updateCurrentUserPreferences(String userId, UserPreferencesUpdateSnapshot request);

    List<ApiKeySummarySnapshot> listCurrentUserApiKeys(String userId);

    CreateApiKeySnapshot createCurrentUserApiKey(String userId, String label);

    void deleteCurrentUserApiKey(String userId, String apiKeyId);

    LoginActivityPageSnapshot listCurrentUserLoginActivities(String userId, int page, int size);

    List<SignalItemSnapshot> listSignals(String status, int limit);

    ConnectivityHealthSnapshot getSystemConnectivityHealth();

    ExecutionLogPageSnapshot listSystemExecutionLogs(String cursor, int limit);

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

    record DashboardOverviewSnapshot(
            double totalEquity,
            double openPnl,
            double winRate,
            int activeBots
            ) {

    }

    record ExchangeAllocationSnapshot(String exchange, double percentage) {

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

    record TimeSeriesPointSnapshot(LocalDateTime timestamp, double value) {

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
            double maxDrawdown
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

    record PaperSessionSummarySnapshot(
            String sessionId,
            String status,
            double virtualBalance,
            double openPnl,
            double buyingPower
            ) {

    }

    record PaperSignalSnapshot(
            String signalId,
            String botId,
            String assetPair,
            String side,
            double confidence,
            String status,
            LocalDateTime generatedAt
            ) {

    }

    record PaperOrderCreateSnapshot(
            String assetPair,
            String orderType,
            String side,
            double quantity,
            Double limitPrice
            ) {

    }

    record PaperOrderSnapshot(
            String orderId,
            String status,
            double executedPrice
            ) {

    }

    record PaperSessionStateSnapshot(
            String sessionId,
            String status
            ) {

    }

    record PaperExecutionLogItemSnapshot(
            LocalDateTime timestamp,
            String level,
            String message
            ) {

    }

    record CursorPaginationMetaSnapshot(
            String cursor,
            String nextCursor,
            int limit,
            boolean hasMore
            ) {

    }

    record PaperExecutionLogPageSnapshot(
            List<PaperExecutionLogItemSnapshot> items,
            CursorPaginationMetaSnapshot meta
            ) {

    }

    record UserProfileSnapshot(String userId, String username, String email, String role) {

    }

    record UserPreferencesUpdateSnapshot(
            String timezone,
            String locale,
            Boolean emailNotificationsEnabled
            ) {

    }

    record UserPreferencesSnapshot(
            String timezone,
            String locale,
            boolean emailNotificationsEnabled
            ) {

    }

    record ApiKeySummarySnapshot(
            String apiKeyId,
            String label,
            String maskedKey,
            LocalDateTime createdAt,
            LocalDateTime lastUsedAt
            ) {

    }

    record CreateApiKeySnapshot(
            String apiKeyId,
            String key,
            String label
            ) {

    }

    record LoginActivitySnapshot(
            LocalDateTime occurredAt,
            String ipAddress,
            String userAgent,
            boolean success
            ) {

    }

    record LoginActivityPageSnapshot(
            List<LoginActivitySnapshot> items,
            OffsetPaginationMetaSnapshot meta
            ) {

    }

    record OffsetPaginationMetaSnapshot(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
            ) {

    }

    record SignalItemSnapshot(
            String signalId,
            String botId,
            String exchangeSlug,
            String symbol,
            String action,
            double price,
            String status,
            LocalDateTime generatedTimestamp
            ) {

    }

    record ConnectivityHealthDependencySnapshot(String name, String status, int latencyMs) {

    }

    record ConnectivityHealthSnapshot(
            String overallStatus,
            LocalDateTime checkedAt,
            List<ConnectivityHealthDependencySnapshot> dependencies
            ) {

    }

    record ExecutionLogItemSnapshot(LocalDateTime timestamp, String level, String source, String message) {

    }

    record ExecutionLogPageSnapshot(String cursor, List<ExecutionLogItemSnapshot> items) {

    }
}

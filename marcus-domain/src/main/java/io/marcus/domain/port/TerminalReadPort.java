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

        /**
         * Per-bot integration health snapshot. Contains dependency checks and last signal timestamp.
         *
         * @param botId bot identifier
         * @return integration health snapshot for the bot
         */
        BotIntegrationHealthSnapshot getBotIntegrationHealth(String botId);

        /**
         * Delivery summary for a subscription (recent window, e.g. 24h).
         *
         * @param subscriptionId subscription identifier
         * @return delivery summary snapshot
         */
        SubscriptionDeliverySummarySnapshot getSubscriptionDeliverySummary(String subscriptionId);

        /**
         * Bot credentials summary (masked only). Raw secret is not retrievable via this API.
         *
         * @param botId bot identifier
         * @return credentials snapshot
         */
        ApiKeySnapshot getBotCredentials(String botId);

    ExecutionLogPageSnapshot listSystemExecutionLogs(String cursor, int limit);

    // Pha 1: Decision Dashboard - Portfolio-level queries
    /**
     * Portfolio overview snapshot for Decision Dashboard header stats.
     * Aggregates all active subscriptions for current user into consolidated
     * metrics.
     *
     * @param userId current trader
     * @return aggregated portfolio metrics (not formula-based)
     */
    PortfolioOverviewSnapshot getPortfolioOverview(String userId);

    /**
     * Subscription list enriched with decision reason tags. Each subscription
     * includes decision context (win rate, drawdown, reason tag, explanation).
     * Results are sorted by reason priority (HIGH_RISK first).
     *
     * @param userId current trader
     * @param statusFilter filter: null=ALL, "ACTIVE", "AT_RISK"
     * @return sorted list of decision-enriched subscriptions
     */
    List<SubscriptionDecisionSnapshot> getSubscriptionDecisions(String userId, String statusFilter);

    /**
     * Single subscription enrichment for drill-down/detail context.
     *
     * @param subscriptionId subscription to fetch
     * @return decision-enriched subscription snapshot
     */
    SubscriptionDecisionSnapshot getSubscriptionDecision(String subscriptionId);

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

    record BotIntegrationHealthSnapshot(
            String overallStatus,
            LocalDateTime lastCheckedAt,
            List<ConnectivityHealthDependencySnapshot> dependencies,
            LocalDateTime lastSignalAt,
            String message
    ) {

    }

    record SubscriptionDeliverySummarySnapshot(
            long successCount24h,
            long failureCount24h,
            LocalDateTime lastDeliveryAt,
            DeliveryErrorSnapshot lastError
    ) {

    }

    record DeliveryErrorSnapshot(String code, String message) {

    }

    record ApiKeySnapshot(String apiKeyId, String maskedKey, boolean rawSecretRetrievable) {

    }

    record ExecutionLogItemSnapshot(LocalDateTime timestamp, String level, String source, String message) {

    }

    record ExecutionLogPageSnapshot(String cursor, List<ExecutionLogItemSnapshot> items) {

    }

    // Pha 1: Decision Dashboard - Portfolio snapshot records
    /**
     * Portfolio overview for Decision Dashboard header. Aggregated metrics
     * across all user subscriptions.
     */
    record PortfolioOverviewSnapshot(
            int activeBotsCount,
            double totalSubscribedCapital,
            double aggregateWinRate24h,
            int atRiskSubscriptionCount,
            double totalEquity,
            double aggregateOpenPnL,
            LocalDateTime lastUpdated
            ) {

    }

    /**
     * Decision reason tag for bot subscription (strategy). Determines visual
     * prominence and action priority on Decision Dashboard.
     */
    enum DecisionReason {
        SOLID_PERFORMER, // Win rate > 60% AND drawdown > -5%
        NEEDS_REVIEW, // Recent failure rate > 20% OR drawdown -5% to -10%
        HIGH_RISK, // Drawdown < -10%
        SLIPPING            // No recent signals (> 4h silence)
    }

    /**
     * Subscription decision snapshot for Decision Dashboard card. Includes
     * performance metrics, risk indicators, and decision reason tag.
     */
    record SubscriptionDecisionSnapshot(
            String subscriptionId,
            String botId,
            String botName,
            String botIcon,
            String status,
            double currentPnL,
            double pnlPercent,
            double drawdownPercent,
            double winRate,
            int signalCount24h,
            int successfulSignals24h,
            DecisionReason reason,
            String reasonExplanation,
            double riskScore, // 0.0 (safe) to 1.0 (high risk)
            int subscribedSinceDay,
            int daysAtRisk, // consecutive days with drawdown
            LocalDateTime lastSignal,
            String exchange
            ) {

    }
}

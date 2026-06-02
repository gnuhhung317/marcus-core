package io.marcus.domain.port;

import java.time.LocalDateTime;
import java.util.List;

public interface PortfolioReadPort {

        record TimeSeriesPointSnapshot(LocalDateTime timestamp, double value) {
        }

        record CursorPaginationMetaSnapshot(
                        String cursor,
                        String nextCursor,
                        int limit,
                        boolean hasMore) {
        }

        record SignalItemSnapshot(
                        String signalId,
                        String botId,
                        String exchangeSlug,
                        String symbol,
                        String action,
                        double price,
                        String status,
                        LocalDateTime generatedTimestamp,
                        Boolean isSimulated) {
        }

        /**
         * KPI summary for the Bot Operator Hub header.
         * Delivery rate = successCount24h / max(totalDispatched24h, 1) * 100.
         */
        record BotSignalSummarySnapshot(
                        long totalSignals24h,
                        long successfulSignals24h,
                        double deliveryRatePercent,
                        int activeSubscriberCount,
                        LocalDateTime lastSignalAt) {
        }

        record ConnectivityHealthSnapshot(
                        String overallStatus,
                        LocalDateTime checkedAt) {
        }

        record BotIntegrationHealthSnapshot(
                        String overallStatus,
                        LocalDateTime lastCheckedAt,
                        LocalDateTime lastSignalAt,
                        String message) {
        }

        record SubscriptionDeliverySummarySnapshot(
                        long successCount24h,
                        long failureCount24h,
                        LocalDateTime lastDeliveryAt,
                        DeliveryErrorSnapshot lastError) {
        }

        record DeliveryErrorSnapshot(String code, String message) {
        }

        record ApiKeySnapshot(String apiKeyId, String maskedKey, boolean rawSecretRetrievable) {
        }

        record ExecutionLogItemSnapshot(LocalDateTime timestamp, String level, String source, String message) {
        }

        record ExecutionLogPageSnapshot(String cursor, List<ExecutionLogItemSnapshot> items) {
        }

        record PortfolioOverviewSnapshot(
                        int activeBotsCount,
                        double totalSubscribedCapital,
                        double aggregateWinRate24h,
                        int atRiskSubscriptionCount,
                        double totalEquity,
                        double aggregateOpenPnL,
                        LocalDateTime lastUpdated) {
        }

        enum DecisionReason {
                SOLID_PERFORMER,
                NEEDS_REVIEW,
                HIGH_RISK,
                SLIPPING
        }

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
                        double riskScore,
                        int subscribedSinceDay,
                        int daysAtRisk,
                        LocalDateTime lastSignal,
                        String exchange) {
        }

        List<TimeSeriesPointSnapshot> listDashboardEquitySeries(String userId, String range);

        List<SignalItemSnapshot> listSignals(String status, int limit);

        /**
         * List signals for a specific bot (developer view).
         *
         * @param botId  the bot identifier
         * @param status lifecycle filter (ALL, RECEIVED, DISPATCHED, ACKNOWLEDGED, FAILED)
         * @param limit  max rows to return
         */
        List<SignalItemSnapshot> listSignalsByBot(String botId, String status, int limit);

        /**
         * Look up a single signal by its unique identifier.
         *
         * @param signalId the signal identifier
         * @return single-element list or empty list if not found
         */
        List<SignalItemSnapshot> listSignalsBySignalId(String signalId);

        /**
         * Aggregate KPI summary for the developer dashboard header.
         *
         * @param botId the bot identifier
         */
        BotSignalSummarySnapshot getBotSignalSummary(String botId);

        ConnectivityHealthSnapshot getSystemConnectivityHealth();

        BotIntegrationHealthSnapshot getBotIntegrationHealth(String botId);

        SubscriptionDeliverySummarySnapshot getSubscriptionDeliverySummary(String subscriptionId);

        ApiKeySnapshot getBotCredentials(String botId);

        ExecutionLogPageSnapshot listSystemExecutionLogs(String cursor, int limit);

        PortfolioOverviewSnapshot getPortfolioOverview(String userId);

        List<SubscriptionDecisionSnapshot> getSubscriptionDecisions(String userId, String statusFilter);

        SubscriptionDecisionSnapshot getSubscriptionDecision(String subscriptionId);
}

package io.marcus.domain.port;

import io.marcus.domain.port.UserProfileReadPort.OffsetPaginationMetaSnapshot;
import java.time.LocalDateTime;
import java.util.List;

public interface PortfolioReadPort {

        record TimeSeriesPointSnapshot(LocalDateTime timestamp, double value) {
        }

        record PaperSessionSummarySnapshot(
                        String sessionId,
                        String status,
                        double virtualBalance,
                        double openPnl,
                        double buyingPower) {
        }

        record PaperSignalSnapshot(
                        String signalId,
                        String botId,
                        String assetPair,
                        String side,
                        double confidence,
                        String status,
                        LocalDateTime generatedAt) {
        }

        record PaperOrderCreateSnapshot(
                        String assetPair,
                        String orderType,
                        String side,
                        double quantity,
                        Double limitPrice) {
        }

        record PaperOrderSnapshot(
                        String orderId,
                        String status,
                        double executedPrice) {
        }

        record PaperSessionStateSnapshot(
                        String sessionId,
                        String status) {
        }

        record PaperExecutionLogItemSnapshot(
                        LocalDateTime timestamp,
                        String level,
                        String message) {
        }

        record CursorPaginationMetaSnapshot(
                        String cursor,
                        String nextCursor,
                        int limit,
                        boolean hasMore) {
        }

        record PaperExecutionLogPageSnapshot(
                        List<PaperExecutionLogItemSnapshot> items,
                        CursorPaginationMetaSnapshot meta) {
        }

        record SignalItemSnapshot(
                        String signalId,
                        String botId,
                        String exchangeSlug,
                        String symbol,
                        String action,
                        double price,
                        String status,
                        LocalDateTime generatedTimestamp) {
        }

        record ConnectivityHealthDependencySnapshot(String name, String status, int latencyMs) {
        }

        record ConnectivityHealthSnapshot(
                        String overallStatus,
                        LocalDateTime checkedAt,
                        List<ConnectivityHealthDependencySnapshot> dependencies) {
        }

        record BotIntegrationHealthSnapshot(
                        String overallStatus,
                        LocalDateTime lastCheckedAt,
                        List<ConnectivityHealthDependencySnapshot> dependencies,
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

        PaperSessionSummarySnapshot getPaperSessionSummary(String userId);

        List<PaperSignalSnapshot> listPaperSignals(String status, int limit);

        PaperExecutionLogPageSnapshot listPaperExecutionLogs(String userId, String cursor, int limit);

        PaperOrderSnapshot createPaperOrder(String userId, PaperOrderCreateSnapshot request);

        PaperSessionStateSnapshot pausePaperSession(String userId);

        PaperSessionStateSnapshot resumePaperSession(String userId);

        List<SignalItemSnapshot> listSignals(String status, int limit);

        ConnectivityHealthSnapshot getSystemConnectivityHealth();

        BotIntegrationHealthSnapshot getBotIntegrationHealth(String botId);

        SubscriptionDeliverySummarySnapshot getSubscriptionDeliverySummary(String subscriptionId);

        ApiKeySnapshot getBotCredentials(String botId);

        ExecutionLogPageSnapshot listSystemExecutionLogs(String cursor, int limit);

        PortfolioOverviewSnapshot getPortfolioOverview(String userId);

        List<SubscriptionDecisionSnapshot> getSubscriptionDecisions(String userId, String statusFilter);

        SubscriptionDecisionSnapshot getSubscriptionDecision(String subscriptionId);
}

package io.marcus.domain.port;

public record PortfolioSyncContext(
        String userId,
        String userSubscriptionId,
        String botId,
        String wsToken
) {
}

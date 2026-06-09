package io.marcus.domain.port;

public interface LeaderboardMetricsRefreshPort {

    void recalculateForBot(String botId);
}

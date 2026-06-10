package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.vo.LeaderboardDataSource;
import io.marcus.domain.vo.LeaderboardRankMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListLeaderboardBotsUseCase {

    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public BotDiscoveryReadPort.LeaderboardBotsPageSnapshot execute(
            String dataSource,
            String market,
            String asset,
            String rankMetric,
            int page,
            int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));

        return botDiscoveryReadPort.listLeaderboardBots(
                LeaderboardDataSource.fromString(dataSource),
                market,
                asset,
                LeaderboardRankMetric.fromString(rankMetric),
                normalizedPage,
                normalizedSize
        );
    }
}

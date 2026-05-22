package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListLeaderboardStrategiesUseCase {

    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public BotDiscoveryReadPort.LeaderboardStrategiesPageSnapshot execute(
            String timeframe,
            String market,
            String asset,
            String rankMetric,
            int page,
            int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));

        return botDiscoveryReadPort.listLeaderboardStrategies(
                timeframe,
                market,
                asset,
                rankMetric,
                normalizedPage,
                normalizedSize
        );
    }
}

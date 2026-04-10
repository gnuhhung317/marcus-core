package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListLeaderboardStrategiesUseCase {

    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.LeaderboardStrategiesPageSnapshot execute(
            String timeframe,
            String market,
            String asset,
            String rankMetric,
            int page,
            int size
    ) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, 200));
        return terminalReadPort.listLeaderboardStrategies(
                timeframe,
                market,
                asset,
                rankMetric,
                normalizedPage,
                normalizedSize
        );
    }
}

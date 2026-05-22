package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListLeaderboardSpotlightsUseCase {

    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public List<BotDiscoveryReadPort.StrategySpotlightSnapshot> execute() {
        return botDiscoveryReadPort.listLeaderboardSpotlights();
    }
}

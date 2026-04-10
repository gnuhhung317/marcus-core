package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListLeaderboardSpotlightsUseCase {

    private final TerminalReadPort terminalReadPort;

    public List<TerminalReadPort.StrategySpotlightSnapshot> execute() {
        return terminalReadPort.listLeaderboardSpotlights();
    }
}

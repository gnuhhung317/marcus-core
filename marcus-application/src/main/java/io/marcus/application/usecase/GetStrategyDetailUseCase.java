package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStrategyDetailUseCase {

    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public BotDiscoveryReadPort.StrategyDetailSnapshot execute(String strategyId) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("Strategy id is required");
        }
        return botDiscoveryReadPort.getStrategyDetail(strategyId.trim());
    }
}

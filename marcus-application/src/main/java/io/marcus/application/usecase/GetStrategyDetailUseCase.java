package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStrategyDetailUseCase {

    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.StrategyDetailSnapshot execute(String strategyId) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("Strategy id is required");
        }
        return terminalReadPort.getStrategyDetail(strategyId.trim());
    }
}

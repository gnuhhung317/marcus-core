package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStrategyMetricsUseCase {

    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.StrategyMetricsSnapshot execute(String strategyId, String feeMode) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("Strategy id is required");
        }

        String normalizedFeeMode = feeMode == null || feeMode.isBlank() ? "RAW" : feeMode.trim();
        return terminalReadPort.getStrategyMetrics(strategyId.trim(), normalizedFeeMode);
    }
}

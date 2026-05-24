package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetStrategyMetricsUseCase {

    private static final Set<String> SUPPORTED_FEE_MODES = Set.of("RAW", "AFTER_FEES");

    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public BotDiscoveryReadPort.StrategyMetricsSnapshot execute(String strategyId, String feeMode) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("Strategy id is required");
        }

        String normalizedFeeMode = feeMode == null || feeMode.isBlank()
                ? "RAW"
                : feeMode.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_FEE_MODES.contains(normalizedFeeMode)) {
            throw new IllegalArgumentException("Unsupported fee mode: " + feeMode);
        }
        return botDiscoveryReadPort.getStrategyMetrics(strategyId.trim(), normalizedFeeMode);
    }
}

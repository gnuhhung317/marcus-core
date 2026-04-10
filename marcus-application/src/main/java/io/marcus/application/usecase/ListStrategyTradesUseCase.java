package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ListStrategyTradesUseCase {

    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.TradeLogPageSnapshot execute(String strategyId, int page, int size, String asset) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("Strategy id is required");
        }

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        String normalizedAsset = asset == null || asset.isBlank()
            ? null
            : asset.trim().toUpperCase(Locale.ROOT);

        return terminalReadPort.listStrategyTrades(strategyId.trim(), normalizedPage, normalizedSize, normalizedAsset);
    }
}

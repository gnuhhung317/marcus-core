package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetStrategyPerformanceSeriesUseCase {

    private final TerminalReadPort terminalReadPort;

    public List<TerminalReadPort.TimeSeriesPointSnapshot> execute(String strategyId, String range) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("Strategy id is required");
        }
        if (range == null || range.isBlank()) {
            throw new IllegalArgumentException("Range is required");
        }

        return terminalReadPort.listStrategyPerformanceSeries(strategyId.trim(), range.trim());
    }
}

package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetStrategyPerformanceSeriesUseCase {

    private static final Set<String> SUPPORTED_RANGES = Set.of("1D", "1W", "1M", "YTD", "ALL");

    private final TerminalReadPort terminalReadPort;

    public List<TerminalReadPort.TimeSeriesPointSnapshot> execute(String strategyId, String range) {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("Strategy id is required");
        }
        if (range == null || range.isBlank()) {
            throw new IllegalArgumentException("Range is required");
        }

        String normalizedRange = range.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_RANGES.contains(normalizedRange)) {
            throw new IllegalArgumentException("Unsupported range: " + range);
        }

        return terminalReadPort.listStrategyPerformanceSeries(strategyId.trim(), normalizedRange);
    }
}

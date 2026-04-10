package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetDashboardEquitySeriesUseCase {

    private static final Set<String> SUPPORTED_RANGES = Set.of("1D", "1W", "1M", "YTD", "ALL");

    private final IdentityService identityService;
    private final TerminalReadPort terminalReadPort;

    public List<TerminalReadPort.TimeSeriesPointSnapshot> execute(String range) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));
        if (range == null || range.isBlank()) {
            throw new IllegalArgumentException("Range is required");
        }

        String normalizedRange = range.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_RANGES.contains(normalizedRange)) {
            throw new IllegalArgumentException("Unsupported range: " + range);
        }
        return terminalReadPort.listDashboardEquitySeries(userId, normalizedRange);
    }
}

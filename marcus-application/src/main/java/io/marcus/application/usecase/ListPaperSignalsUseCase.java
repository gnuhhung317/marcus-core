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
public class ListPaperSignalsUseCase {

    private static final Set<String> SUPPORTED_STATUSES = Set.of("ACTIVE", "EXECUTED", "EXPIRED", "ALL");

    private final IdentityService identityService;
    private final TerminalReadPort terminalReadPort;

    public List<TerminalReadPort.PaperSignalSnapshot> execute(String status, int limit) {
        identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        String normalizedStatus = status == null || status.isBlank()
                ? "ALL"
                : status.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported status: " + status);
        }
        return terminalReadPort.listPaperSignals(normalizedStatus, normalizedLimit);
    }
}

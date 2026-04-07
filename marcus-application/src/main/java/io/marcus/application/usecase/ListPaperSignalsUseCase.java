package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPaperSignalsUseCase {

    private final TerminalReadPort terminalReadPort;

    public List<TerminalReadPort.PaperSignalSnapshot> execute(String status, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        String normalizedStatus = status == null || status.isBlank() ? "ALL" : status.trim();
        return terminalReadPort.listPaperSignals(normalizedStatus, normalizedLimit);
    }
}

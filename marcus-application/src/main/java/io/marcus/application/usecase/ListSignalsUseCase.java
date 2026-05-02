package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListSignalsUseCase {

    private final TerminalReadPort terminalReadPort;

    public List<TerminalReadPort.SignalItemSnapshot> execute(String status, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        String normalizedStatus = status == null || status.isBlank() ? "ALL" : status.trim();
        return terminalReadPort.listSignals(normalizedStatus, normalizedLimit);
    }
}

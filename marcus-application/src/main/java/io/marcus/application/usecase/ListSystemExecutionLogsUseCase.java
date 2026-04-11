package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListSystemExecutionLogsUseCase {

    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.ExecutionLogPageSnapshot execute(String cursor, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        return terminalReadPort.listSystemExecutionLogs(cursor, normalizedLimit);
    }
}

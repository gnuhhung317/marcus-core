package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSystemConnectivityHealthUseCase {

    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.ConnectivityHealthSnapshot execute() {
        return terminalReadPort.getSystemConnectivityHealth();
    }
}

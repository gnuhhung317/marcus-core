package io.marcus.application.usecase;

import io.marcus.domain.port.PortfolioReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListSystemExecutionLogsUseCase {

    private final PortfolioReadPort portfolioReadPort;

    public PortfolioReadPort.ExecutionLogPageSnapshot execute(String cursor, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        return portfolioReadPort.listSystemExecutionLogs(cursor, normalizedLimit);
    }
}

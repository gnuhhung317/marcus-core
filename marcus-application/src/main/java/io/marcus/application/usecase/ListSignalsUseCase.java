package io.marcus.application.usecase;

import io.marcus.domain.port.PortfolioReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListSignalsUseCase {

    private final PortfolioReadPort portfolioReadPort;

    public List<PortfolioReadPort.SignalItemSnapshot> execute(String status, int limit, String botId, String signalId) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        
        if (signalId != null && !signalId.isBlank()) {
            return portfolioReadPort.listSignalsBySignalId(signalId);
        }

        String normalizedStatus = status == null || status.isBlank() ? "ALL" : status.trim();

        if (botId != null && !botId.isBlank()) {
            return portfolioReadPort.listSignalsByBot(botId, normalizedStatus, normalizedLimit);
        }

        return portfolioReadPort.listSignals(normalizedStatus, normalizedLimit);
    }
}

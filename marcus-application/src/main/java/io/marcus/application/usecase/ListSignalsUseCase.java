package io.marcus.application.usecase;

import io.marcus.domain.port.PortfolioReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListSignalsUseCase {

    private final PortfolioReadPort portfolioReadPort;

    public List<PortfolioReadPort.SignalItemSnapshot> execute(String status, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        String normalizedStatus = status == null || status.isBlank() ? "ALL" : status.trim();
        return portfolioReadPort.listSignals(normalizedStatus, normalizedLimit);
    }
}

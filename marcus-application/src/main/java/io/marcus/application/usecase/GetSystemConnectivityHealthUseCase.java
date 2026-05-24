package io.marcus.application.usecase;

import io.marcus.domain.port.PortfolioReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSystemConnectivityHealthUseCase {

    private final PortfolioReadPort portfolioReadPort;

    public PortfolioReadPort.ConnectivityHealthSnapshot execute() {
        return portfolioReadPort.getSystemConnectivityHealth();
    }
}

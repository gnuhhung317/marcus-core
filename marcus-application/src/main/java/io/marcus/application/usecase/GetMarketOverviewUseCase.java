package io.marcus.application.usecase;

import io.marcus.domain.port.MarketingContentReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMarketOverviewUseCase {

    private final MarketingContentReadPort marketingContentReadPort;

    public MarketingContentReadPort.MarketOverviewSnapshot execute() {
        return marketingContentReadPort.getMarketOverview();
    }
}
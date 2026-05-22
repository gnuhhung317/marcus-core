package io.marcus.application.usecase;

import io.marcus.domain.port.PortfolioReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSubscriptionDeliverySummaryUseCase {

    private final PortfolioReadPort portfolioReadPort;

    public PortfolioReadPort.SubscriptionDeliverySummarySnapshot execute(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("Subscription id is required");
        }

        return portfolioReadPort.getSubscriptionDeliverySummary(subscriptionId.trim());
    }
}

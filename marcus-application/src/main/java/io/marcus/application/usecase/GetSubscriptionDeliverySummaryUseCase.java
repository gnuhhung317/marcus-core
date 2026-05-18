package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSubscriptionDeliverySummaryUseCase {

    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.SubscriptionDeliverySummarySnapshot execute(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("Subscription id is required");
        }

        return terminalReadPort.getSubscriptionDeliverySummary(subscriptionId.trim());
    }
}

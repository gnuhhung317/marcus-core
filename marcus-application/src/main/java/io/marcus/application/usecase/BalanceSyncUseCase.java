package io.marcus.application.usecase;

import io.marcus.application.dto.BalanceSyncRequest;
import io.marcus.domain.port.PortfolioBalanceSyncData;
import io.marcus.domain.port.PortfolioSyncContext;
import io.marcus.domain.port.PortfolioSyncPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceSyncUseCase {

    private final PortfolioSyncPort portfolioSyncPort;

    public void execute(PortfolioSyncContext context, BalanceSyncRequest request) {
        if (context == null || context.userId() == null || context.userId().isBlank()) {
            log.warn("Received balance sync attempt without valid user context");
            return;
        }
        if (request == null) {
            log.warn("Received balance sync with null request payload for user: {}", context.userId());
            return;
        }

        portfolioSyncPort.syncBalance(
                context,
                new PortfolioBalanceSyncData(
                        request.total(),
                        request.available(),
                        request.used(),
                        request.unrealizedPnl(),
                        request.exchange(),
                        request.currency(),
                        request.executionMode()
                )
        );
        log.info("Successfully synced balance for user: {} subscription: {}. Available: {}, Unrealized PnL: {}",
                context.userId(), context.userSubscriptionId(), request.available(), request.unrealizedPnl());
    }
}

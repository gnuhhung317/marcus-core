package io.marcus.application.usecase;

import io.marcus.application.dto.BalanceSyncRequest;
import io.marcus.domain.model.UserPortfolio;
import io.marcus.domain.port.UserPortfolioPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceSyncUseCase {

    private final UserPortfolioPersistencePort userPortfolioPersistencePort;

    @Transactional
    public void execute(String userId, BalanceSyncRequest request) {
        if (userId == null || userId.isBlank()) {
            log.warn("Received balance sync attempt without valid userId");
            return;
        }
        if (request == null) {
            log.warn("Received balance sync with null request payload for user: {}", userId);
            return;
        }

        UserPortfolio portfolio = userPortfolioPersistencePort
                .findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Initializing default user portfolio for userId: {}", userId);
                    return UserPortfolio.createDefault(userId);
                });

        portfolio.updateBalance(
                request.total(),
                request.available(),
                request.unrealizedPnl(),
                request.exchange()
        );

        userPortfolioPersistencePort.save(portfolio);
        userPortfolioPersistencePort.saveHistory(
                userId,
                request.total(),
                request.available(),
                request.used(),
                request.unrealizedPnl(),
                request.exchange()
        );
        log.info("Successfully synced balance and saved history for user: {}. Available: {}, Unrealized PnL: {}",
                userId, request.available(), request.unrealizedPnl());
    }
}

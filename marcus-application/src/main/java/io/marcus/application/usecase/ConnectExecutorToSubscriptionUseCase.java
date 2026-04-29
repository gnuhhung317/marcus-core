package io.marcus.application.usecase;

import io.marcus.application.dto.BotSubscriptionResult;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConnectExecutorToSubscriptionUseCase {

    private final UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    @Transactional
    public Optional<BotSubscriptionResult> execute(String botId, String wsToken) {
        var found = userSubscriptionPersistencePort.findActiveByBotIdAndWsToken(botId, wsToken);
        if (found.isPresent()) {
            UserSubscription subscription = found.get();
            userSubscriptionPersistencePort.markExecutorConnected(subscription.getUserSubscriptionId(), true);
            return Optional.of(new BotSubscriptionResult(subscription.getBotId(), subscription.getWsToken(), "EXECUTOR_CONNECTED"));
        }
        return Optional.empty();
    }
}

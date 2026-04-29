package io.marcus.application.usecase;

import io.marcus.application.dto.BotSubscriptionResult;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListActiveSubscriptionsForBotUseCase {

    private final UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    public List<BotSubscriptionResult> execute(String botId) {
        return userSubscriptionPersistencePort.findActiveByBotId(botId)
                .stream()
                .map(s -> new BotSubscriptionResult(s.getBotId(), s.getWsToken(), s.getStatus().name()))
                .collect(Collectors.toList());
    }
}

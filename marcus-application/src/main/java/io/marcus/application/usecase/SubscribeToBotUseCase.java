package io.marcus.application.usecase;

import io.marcus.application.dto.BotSubscriptionResult;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import io.marcus.domain.vo.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscribeToBotUseCase {

    private final IdentityService identityService;
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    @Transactional
    public BotSubscriptionResult execute(String botId) {
        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (!userRepository.existsByIdAndRole(currentUserId, Role.USER)) {
            throw new ForbiddenOperationException("Only trader can subscribe to bot");
        }

        String normalizedBotId = normalizeBotId(botId);
        if (botRepository.findByBotId(normalizedBotId).isEmpty()) {
            throw new IllegalArgumentException("Bot not found: " + normalizedBotId);
        }

        return userSubscriptionPersistencePort.findActiveByUserIdAndBotId(currentUserId, normalizedBotId)
                .map(existing -> new BotSubscriptionResult(normalizedBotId, existing.getWsToken(), "SUBSCRIBED"))
                .orElseGet(() -> {
                    String wsToken = "ws_" + UUID.randomUUID().toString().replace("-", "");
                    UserSubscription subscription = UserSubscription.builder()
                            .userSubscriptionId("sub_" + UUID.randomUUID().toString().replace("-", ""))
                            .userId(currentUserId)
                            .botId(normalizedBotId)
                            .wsToken(wsToken)
                            .status(SubscriptionStatus.ACTIVE)
                            .startDate(LocalDateTime.now())
                            .build();

                    UserSubscription saved = userSubscriptionPersistencePort.save(subscription);
                    return new BotSubscriptionResult(saved.getBotId(), saved.getWsToken(), "SUBSCRIBED");
                });
    }

    private String normalizeBotId(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }
        return botId.trim();
    }
}

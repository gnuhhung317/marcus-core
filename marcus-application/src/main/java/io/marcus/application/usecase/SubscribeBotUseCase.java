package io.marcus.application.usecase;

import io.marcus.application.dto.SubscribeBotResult;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.BotSubscriberRoutingPort;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.Role;
import io.marcus.domain.vo.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscribeBotUseCase {

    private final IdentityService identityService;
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final UserSubscriptionPersistencePort userSubscriptionPersistencePort;
    private final BotSubscriberRoutingPort botSubscriberRoutingPort;

    @Transactional
    public SubscribeBotResult execute(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }

        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (!userRepository.existsByIdAndRole(currentUserId, Role.USER)) {
            throw new ForbiddenOperationException("Only trader can subscribe bot");
        }

        String trimmedBotId = botId.trim();
        Bot bot = botRepository.findByBotId(trimmedBotId)
                .orElseThrow(() -> new BotNotFoundException("Bot not found: " + trimmedBotId));

        if (bot.getStatus() != BotStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active bot can be subscribed");
        }

        UserSubscription activeSubscription = userSubscriptionPersistencePort
                .findActiveByUserIdAndBotId(currentUserId, trimmedBotId)
                .orElseGet(() -> createSubscription(currentUserId, trimmedBotId));

        botSubscriberRoutingPort.upsertSubscriber(trimmedBotId, currentUserId);

        return SubscribeBotResult.builder()
                .botId(trimmedBotId)
                .wsToken(activeSubscription.getWsToken())
                .status(activeSubscription.getStatus().name())
                .build();
    }

    private UserSubscription createSubscription(String userId, String botId) {
        String wsToken = userSubscriptionPersistencePort.findAnyActiveWsTokenByUserId(userId)
                .orElseGet(this::generateWsToken);

        UserSubscription subscription = UserSubscription.builder()
                .userSubscriptionId("sub_" + UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .botId(botId)
                .wsToken(wsToken)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now())
                .build();

        return userSubscriptionPersistencePort.save(subscription);
    }

    private String generateWsToken() {
        return "ws_" + UUID.randomUUID().toString().replace("-", "");
    }
}

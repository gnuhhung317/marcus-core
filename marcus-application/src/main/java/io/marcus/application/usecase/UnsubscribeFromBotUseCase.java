package io.marcus.application.usecase;

import io.marcus.application.dto.BotSubscriptionResult;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnsubscribeFromBotUseCase {

    private final IdentityService identityService;
    private final UserRepository userRepository;
    private final UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    @Transactional
    public BotSubscriptionResult execute(String botId) {
        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (!userRepository.existsByIdAndRole(currentUserId, Role.USER)) {
            throw new ForbiddenOperationException("Only trader can unsubscribe from bot");
        }

        String normalizedBotId = normalizeBotId(botId);
        userSubscriptionPersistencePort.cancelActiveByUserIdAndBotId(currentUserId, normalizedBotId);

        return new BotSubscriptionResult(normalizedBotId, "", "UNSUBSCRIBED");
    }

    private String normalizeBotId(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }
        return botId.trim();
    }
}

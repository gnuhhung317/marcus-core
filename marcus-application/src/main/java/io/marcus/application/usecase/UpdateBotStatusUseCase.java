package io.marcus.application.usecase;

import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdateBotStatusUseCase {

    private final BotRepository botRepository;
    private final IdentityService identityService;
    private final UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    @Transactional
    public Bot execute(String botId, BotStatus newStatus) {
        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        Bot bot = botRepository.findByBotId(botId)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found with id: " + botId));

        if (!currentUserId.equals(bot.getDeveloperId())) {
            throw new ForbiddenOperationException("Only the developer of the bot can modify its status");
        }

        if (bot.getStatus() == BotStatus.DELETED && newStatus != BotStatus.DELETED) {
            throw new IllegalStateException("Deleted bot cannot be reactivated or modified");
        }

        // Guard: cannot delete if there are active subscribers. Pausing is allowed.
        if (newStatus == BotStatus.DELETED) {
            List<UserSubscription> activeSubs = userSubscriptionPersistencePort.findActiveByBotId(botId);
            if (!activeSubs.isEmpty()) {
                throw new IllegalStateException("Cannot delete bot with active subscriptions (" + activeSubs.size() + " active)");
            }
        }

        bot.setStatus(newStatus);
        if (newStatus == BotStatus.DELETED) {
            bot.setDeletedAt(java.time.LocalDateTime.now());
        }

        return botRepository.save(bot);
    }
}

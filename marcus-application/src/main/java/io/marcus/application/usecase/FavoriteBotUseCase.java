package io.marcus.application.usecase;

import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoriteBotUseCase {

    private final IdentityService identityService;
    private final UserRepository userRepository;
    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public BotDiscoveryReadPort.FavoriteBotSnapshot execute(String botId) {
        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (!userRepository.existsByIdAndRole(currentUserId, Role.TRADER)) {
            throw new ForbiddenOperationException("Only trader can favorite bots");
        }

        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }

        return botDiscoveryReadPort.favoriteBot(currentUserId, botId.trim());
    }
}

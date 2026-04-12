package io.marcus.application.usecase;

import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoriteStrategyUseCase {

    private final IdentityService identityService;
    private final UserRepository userRepository;
    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.FavoriteStrategySnapshot execute(String strategyId) {
        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (!userRepository.existsByIdAndRole(currentUserId, Role.USER)) {
            throw new ForbiddenOperationException("Only trader can favorite strategies");
        }

        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("Strategy id is required");
        }

        return terminalReadPort.favoriteStrategy(currentUserId, strategyId.trim());
    }
}

package io.marcus.application.usecase;

import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.application.mapper.BotDtoMapper;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListDeveloperBotsUseCase {

    private final BotRepository botRepository;
    private final UserRepository userRepository;
    private final IdentityService identityService;
    private final BotDtoMapper botDtoMapper;
    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public List<BotSummaryResult> execute() {
        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (!userRepository.existsByIdAndRole(currentUserId, Role.DEVELOPER)) {
            throw new ForbiddenOperationException("Only developer can list own bots");
        }

        return botRepository.findAllByDeveloperId(currentUserId)
                .stream()
                .map(bot -> {
                    BotDiscoveryReadPort.BotDetailSnapshot detail = null;
                    try {
                        detail = botDiscoveryReadPort.getBotDetail(bot.getBotId(), "AUTO");
                    } catch (Exception e) {
                        // Log or ignore to keep listing robust
                    }
                    return botDtoMapper.toSummaryResult(bot, true, detail);
                })
                .toList();
    }
}

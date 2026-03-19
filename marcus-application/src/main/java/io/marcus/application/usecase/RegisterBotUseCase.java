package io.marcus.application.usecase;

import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.application.mapper.BotDtoMapper;
import io.marcus.domain.model.Bot;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.EncryptionService;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterBotUseCase {

    private final BotRepository botRepository;
    private final EncryptionService encryptionService;
    private final IdentityService identityService;
    private final UserRepository userRepository;
    private final BotDtoMapper botDtoMapper;

    @Transactional
    public BotRegistrationResult execute(RegisterBotRequest botRequest) {
        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (!userRepository.existsByIdAndRole(currentUserId, Role.DEVELOPER)) {
            throw new IllegalArgumentException("Only developer can register bot");
        }

        String apiKey = generateSecureKey("ak");
        String rawSecret = generateSecureKey("sk");
        String secret = encryptionService.encrypt(rawSecret);

        Bot bot = botDtoMapper.toDomain(botRequest);
        bot.setBotId(generateSecureKey("bot"));
        bot.setApiKey(apiKey);
        bot.setSecretKey(secret);
        bot.setDeveloperId(currentUserId);

        bot = botRepository.save(bot);
        return botDtoMapper.toRegistrationResult(bot, rawSecret);
    }

    private String generateSecureKey(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

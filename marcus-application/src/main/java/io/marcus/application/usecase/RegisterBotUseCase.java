package io.marcus.application.usecase;

import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.application.mapper.BotDtoMapper;
import io.marcus.domain.model.Bot;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.EncryptionService;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterBotUseCase {

    private static final String API_KEY_PREFIX = "ak";
    private static final String SECRET_KEY_PREFIX = "sk";
    private static final String BOT_ID_PREFIX = "bot";

    private final BotRepository botRepository;
    private final EncryptionService encryptionService;
    private final IdentityService identityService;
    private final UserRepository userRepository;
    private final BotDtoMapper botDtoMapper;

    @Transactional
    public BotRegistrationResult execute(RegisterBotRequest botRequest) {
        validateRequest(botRequest);

        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (!userRepository.existsByIdAndRole(currentUserId, Role.DEVELOPER)) {
            throw new ForbiddenOperationException("Only developer can register bot");
        }

        String apiKey = generateSecureKey(API_KEY_PREFIX);
        String rawSecret = generateSecureKey(SECRET_KEY_PREFIX);
        String secret = encryptionService.encrypt(rawSecret);

        Bot bot = botDtoMapper.toDomain(botRequest);
        bot.setBotId(generateSecureKey(BOT_ID_PREFIX));
        bot.setApiKey(apiKey);
        bot.setSecretKey(secret);
        bot.setDeveloperId(currentUserId);

        bot = botRepository.save(bot);
        return botDtoMapper.toRegistrationResult(bot, rawSecret);
    }

    private String generateSecureKey(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private void validateRequest(RegisterBotRequest botRequest) {
        if (botRequest == null) {
            throw new IllegalArgumentException("Register bot request is required");
        }

        validateRequiredField("Bot name", botRequest.botName());
        validateRequiredField("Trading pair", botRequest.tradingPair());
        validateRequiredField("Exchange id", botRequest.exchangeId());
    }

    private void validateRequiredField(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}

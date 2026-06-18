package io.marcus.application.usecase;

import io.marcus.application.dto.UpdateBotMetadataRequest;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.BotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateBotMetadataUseCase {

    private final BotRepository botRepository;
    private final IdentityService identityService;

    @Transactional
    public Bot execute(String botId, UpdateBotMetadataRequest request) {
        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        Bot bot = botRepository.findByBotId(botId)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found with id: " + botId));

        if (bot.getStatus() == BotStatus.DELETED) {
            throw new IllegalStateException("Cannot modify metadata of a deleted bot");
        }

        if (!currentUserId.equals(bot.getDeveloperId())) {
            throw new ForbiddenOperationException("Only the developer of the bot can modify its metadata");
        }

        if (request.name() != null) {
            bot.setName(request.name());
        }
        if (request.description() != null) {
            bot.setDescription(request.description());
        }
        if (request.tradingPair() != null) {
            bot.setTradingPair(request.tradingPair());
        }
        if (request.exchangeId() != null) {
            bot.setExchangeId(request.exchangeId());
        }
        if (request.price() != null) {
            bot.setPrice(request.price());
        }
        if (request.riskLevel() != null) {
            bot.setRiskLevel(request.riskLevel());
        }

        return botRepository.save(bot);
    }
}

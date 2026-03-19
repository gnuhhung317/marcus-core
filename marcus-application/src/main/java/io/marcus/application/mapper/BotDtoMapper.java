package io.marcus.application.mapper;

import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.domain.model.Bot;
import io.marcus.domain.vo.BotStatus;
import org.springframework.stereotype.Component;

@Component
public class BotDtoMapper {

    public Bot toDomain(RegisterBotRequest request) {
        if (request == null) {
            return null;
        }
        return Bot.builder()
                .name(request.botName())
                .description(request.description())
                .tradingPair(request.tradingPair())
                .exchangeId(request.exchangeId())
                .status(BotStatus.ACTIVE)
                .build();
    }

    public BotRegistrationResult toRegistrationResult(Bot bot, String rawSecret) {
        if (bot == null) {
            return null;
        }
        return BotRegistrationResult.builder()
                .botId(bot.getBotId())
                .botName(bot.getName())
                .description(bot.getDescription())
                .name(bot.getName()) // Mapping both botName and name as per DTO definition
                .apiKey(bot.getApiKey())
                .rawSecret(rawSecret)
                .status(bot.getStatus() != null ? bot.getStatus().name() : null)
                .tradingPair(bot.getTradingPair())
                .exchange(bot.getExchangeId())
                .build();
    }
}

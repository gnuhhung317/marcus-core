package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.User;
import io.marcus.domain.port.AdminBotPort;
import io.marcus.domain.port.AdminSubscriptionPort;
import io.marcus.domain.port.AdminUserPort;
import io.marcus.domain.vo.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminGetBotDetailUseCase {

    private final AdminBotPort adminBotPort;
    private final AdminUserPort adminUserPort;
    private final AdminSubscriptionPort adminSubscriptionPort;

    public AdminDtos.BotDetail execute(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }

        Bot bot = adminBotPort.findByBotId(botId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Bot not found: " + botId));
        User developer = adminUserPort.findByUserId(bot.getDeveloperId()).orElse(null);

        return new AdminDtos.BotDetail(
                bot.getBotId(),
                bot.getName(),
                bot.getDeveloperId(),
                developer != null ? developer.getUsername() : null,
                bot.getDescription(),
                bot.getStatus(),
                bot.getTradingPair(),
                bot.getExchangeId(),
                bot.getPrice(),
                bot.getRiskLevel(),
                bot.getAssetPairs(),
                bot.getCreatedAt(),
                bot.getUpdatedAt(),
                adminSubscriptionPort.countByBotIdAndStatus(bot.getBotId(), SubscriptionStatus.ACTIVE),
                adminSubscriptionPort.countByBotId(bot.getBotId())
        );
    }
}

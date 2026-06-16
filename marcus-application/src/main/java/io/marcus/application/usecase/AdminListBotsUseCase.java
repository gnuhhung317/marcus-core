package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.User;
import io.marcus.domain.model.PagedResult;
import io.marcus.domain.port.AdminBotPort;
import io.marcus.domain.port.AdminSubscriptionPort;
import io.marcus.domain.port.AdminUserPort;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminListBotsUseCase {

    private final AdminBotPort adminBotPort;
    private final AdminUserPort adminUserPort;
    private final AdminSubscriptionPort adminSubscriptionPort;

    public AdminDtos.PageResponse<AdminDtos.BotRow> execute(String query, BotStatus status, String developerId, int page, int size) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, 100));

        PagedResult<Bot> result = adminBotPort.search(query, status, developerId, normalizedPage, normalizedSize);
        Set<String> developerIds = result.getContent().stream().map(Bot::getDeveloperId).collect(Collectors.toSet());
        Map<String, User> usersById = adminUserPort.findByUserIds(developerIds).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        return new AdminDtos.PageResponse<>(
                result.getContent().stream().map(bot -> new AdminDtos.BotRow(
                        bot.getBotId(),
                        bot.getName(),
                        bot.getDeveloperId(),
                        usersById.getOrDefault(bot.getDeveloperId(), null) != null
                                ? usersById.get(bot.getDeveloperId()).getUsername()
                                : null,
                        bot.getStatus() != null ? bot.getStatus().name() : null,
                        bot.getTradingPair(),
                        bot.getExchangeId(),
                        bot.getCreatedAt(),
                        bot.getUpdatedAt(),
                        adminSubscriptionPort.countByBotIdAndStatus(bot.getBotId(), SubscriptionStatus.ACTIVE)
                )).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );
    }
}

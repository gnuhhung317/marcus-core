package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.domain.model.User;
import io.marcus.domain.model.PagedResult;
import io.marcus.domain.port.AdminSubscriptionPort;
import io.marcus.domain.port.AdminUserPort;
import io.marcus.domain.vo.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminListBotSubscribersUseCase {

    private final AdminSubscriptionPort adminSubscriptionPort;
    private final AdminUserPort adminUserPort;

    public AdminDtos.PageResponse<AdminDtos.BotSubscriberRow> execute(String botId, SubscriptionStatus status, int page, int size) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }

        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        PagedResult<io.marcus.domain.model.UserSubscription> result = adminSubscriptionPort.searchByBotId(botId.trim(), status, normalizedPage, normalizedSize);

        Set<String> userIds = result.getContent().stream().map(sub -> sub.getUserId()).collect(Collectors.toSet());
        Map<String, User> usersById = adminUserPort.findByUserIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        return new AdminDtos.PageResponse<>(
                result.getContent().stream().map(sub -> {
                    User user = usersById.get(sub.getUserId());
                    return new AdminDtos.BotSubscriberRow(
                            sub.getUserSubscriptionId(),
                            sub.getUserId(),
                            user != null ? user.getUsername() : null,
                            user != null ? user.getEmail() : null,
                            sub.getStatus(),
                            sub.isExecutorConnected(),
                            sub.getPackageId(),
                            sub.getStartDate(),
                            sub.getEndDate(),
                            sub.getCanceledByAdminId(),
                            sub.getCancellationReason(),
                            sub.getCanceledAt()
                    );
                }).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );
    }
}

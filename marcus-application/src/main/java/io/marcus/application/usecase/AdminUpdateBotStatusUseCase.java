package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.AdminBotPort;
import io.marcus.domain.port.AdminSubscriptionPort;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUpdateBotStatusUseCase {

    private final AdminBotPort adminBotPort;
    private final AdminSubscriptionPort adminSubscriptionPort;
    private final IdentityService identityService;
    private final AdminRecordAuditEventUseCase adminRecordAuditEventUseCase;

    @Transactional
    public AdminDtos.BotRow execute(String botId, AdminDtos.UpdateBotStatusRequest request) {
        String adminUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }

        Bot bot = adminBotPort.findByBotId(botId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Bot not found: " + botId));
        Map<String, Object> before = snapshot(bot);

        if (request.status() == BotStatus.DELETED || request.status() == BotStatus.PAUSED) {
            List<UserSubscription> activeSubs = adminSubscriptionPort.findByBotIdAndStatus(bot.getBotId(), SubscriptionStatus.ACTIVE);
            if (request.cancelActiveSubscriptions()) {
                activeSubs.forEach(sub -> {
                    Map<String, Object> subscriptionBefore = snapshotSubscription(sub);
                    adminSubscriptionPort.forceCancel(
                            sub.getUserSubscriptionId(),
                            adminUserId,
                            request.reason().trim()
                    );
                    UserSubscription canceled = adminSubscriptionPort.findByUserSubscriptionId(sub.getUserSubscriptionId()).orElse(sub);
                    adminRecordAuditEventUseCase.execute(
                            adminUserId,
                            "SUBSCRIPTION_FORCE_CANCELED",
                            "SUBSCRIPTION",
                            canceled.getUserSubscriptionId(),
                            request.reason().trim(),
                            subscriptionBefore,
                            snapshotSubscription(canceled)
                    );
                });
            }
        }

        bot.setStatus(request.status());
        Bot saved = adminBotPort.save(bot);
        adminRecordAuditEventUseCase.execute(
                adminUserId,
                "BOT_STATUS_UPDATED",
                "BOT",
                saved.getBotId(),
                request.reason().trim(),
                before,
                snapshot(saved)
        );

        return new AdminDtos.BotRow(
                saved.getBotId(),
                saved.getName(),
                saved.getDeveloperId(),
                null,
                saved.getStatus() != null ? saved.getStatus().name() : null,
                saved.getTradingPair(),
                saved.getExchangeId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                adminSubscriptionPort.countByBotIdAndStatus(saved.getBotId(), SubscriptionStatus.ACTIVE)
        );
    }

    private Map<String, Object> snapshot(Bot bot) {
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("botId", bot.getBotId());
        snapshot.put("name", bot.getName());
        snapshot.put("developerId", bot.getDeveloperId());
        snapshot.put("status", bot.getStatus() != null ? bot.getStatus().name() : null);
        snapshot.put("tradingPair", bot.getTradingPair());
        snapshot.put("exchangeId", bot.getExchangeId());
        return snapshot;
    }

    private Map<String, Object> snapshotSubscription(UserSubscription subscription) {
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("userSubscriptionId", subscription.getUserSubscriptionId());
        snapshot.put("userId", subscription.getUserId());
        snapshot.put("botId", subscription.getBotId());
        snapshot.put("status", subscription.getStatus() != null ? subscription.getStatus().name() : null);
        snapshot.put("executorConnected", subscription.isExecutorConnected());
        snapshot.put("canceledByAdminId", subscription.getCanceledByAdminId());
        snapshot.put("cancellationReason", subscription.getCancellationReason());
        return snapshot;
    }
}

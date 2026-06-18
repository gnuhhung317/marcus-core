package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.AdminSubscriptionPort;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminForceCancelSubscriptionUseCase {

    private final AdminSubscriptionPort adminSubscriptionPort;
    private final IdentityService identityService;
    private final AdminRecordAuditEventUseCase adminRecordAuditEventUseCase;

    @Transactional
    public AdminDtos.BotSubscriberRow execute(String userSubscriptionId, AdminDtos.ForceCancelSubscriptionRequest request) {
        String adminUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (userSubscriptionId == null || userSubscriptionId.isBlank()) {
            throw new IllegalArgumentException("Subscription id is required");
        }
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }

        UserSubscription subscription = adminSubscriptionPort.findByUserSubscriptionId(userSubscriptionId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + userSubscriptionId));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ForbiddenOperationException("Only active subscriptions can be force canceled");
        }

        Map<String, Object> before = snapshot(subscription);
        adminSubscriptionPort.forceCancel(subscription.getUserSubscriptionId(), adminUserId, request.reason().trim());
        UserSubscription saved = adminSubscriptionPort.findByUserSubscriptionId(subscription.getUserSubscriptionId())
                .orElseThrow(() -> new IllegalStateException("Subscription update failed"));

        adminRecordAuditEventUseCase.execute(
                adminUserId,
                "SUBSCRIPTION_FORCE_CANCELED",
                "SUBSCRIPTION",
                saved.getUserSubscriptionId(),
                request.reason().trim(),
                before,
                snapshot(saved)
        );

        return new AdminDtos.BotSubscriberRow(
                saved.getUserSubscriptionId(),
                saved.getUserId(),
                null,
                null,
                saved.getStatus(),
                saved.isExecutorConnected(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getCanceledByAdminId(),
                saved.getCancellationReason(),
                saved.getCanceledAt()
        );
    }

    private Map<String, Object> snapshot(UserSubscription subscription) {
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

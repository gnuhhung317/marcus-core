package io.marcus.infrastructure.persistence.mapper;

import io.marcus.domain.model.UserSubscription;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import org.springframework.stereotype.Component;

@Component
public class UserSubscriptionMapper {

    public UserSubscription toDomain(UserSubscriptionEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserSubscription.builder()
                .userSubscriptionId(entity.getUserSubscriptionId())
                .userId(entity.getUserId())
                .botId(entity.getBotId())
                .wsToken(entity.getWsToken())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .executorConnected(entity.isExecutorConnected())
                .canceledByAdminId(entity.getCanceledByAdminId())
                .cancellationReason(entity.getCancellationReason())
                .canceledAt(entity.getCanceledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public UserSubscriptionEntity toEntity(UserSubscription domain) {
        if (domain == null) {
            return null;
        }

        return UserSubscriptionEntity.builder()
                .userSubscriptionId(domain.getUserSubscriptionId())
                .userId(domain.getUserId())
                .botId(domain.getBotId())
                .wsToken(domain.getWsToken())
                .status(domain.getStatus())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .executorConnected(domain.isExecutorConnected())
                .canceledByAdminId(domain.getCanceledByAdminId())
                .cancellationReason(domain.getCancellationReason())
                .canceledAt(domain.getCanceledAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}

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
                .packageId(entity.getPackageId())
                .wsToken(entity.getWsToken())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .executorConnected(entity.isExecutorConnected())
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
                .packageId(domain.getPackageId())
                .wsToken(domain.getWsToken())
                .status(domain.getStatus())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .executorConnected(domain.isExecutorConnected())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}

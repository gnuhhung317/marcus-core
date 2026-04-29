package io.marcus.infrastructure.persistence;

import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, String> {

    List<UserSubscriptionEntity> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, SubscriptionStatus status);

    Optional<UserSubscriptionEntity> findFirstByUserIdAndBotIdAndStatusOrderByCreatedAtDesc(
            String userId,
            String botId,
            SubscriptionStatus status
    );

    List<UserSubscriptionEntity> findByBotIdAndStatusOrderByCreatedAtDesc(String botId, SubscriptionStatus status);

    Optional<UserSubscriptionEntity> findFirstByUserSubscriptionId(String userSubscriptionId);
}

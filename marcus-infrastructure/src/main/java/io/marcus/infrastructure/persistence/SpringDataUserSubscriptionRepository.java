package io.marcus.infrastructure.persistence;

import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, String> {

    Optional<UserSubscriptionEntity> findByUserIdAndBotIdAndStatus(String userId, String botId, SubscriptionStatus status);

    List<UserSubscriptionEntity> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, SubscriptionStatus status);

    Optional<UserSubscriptionEntity> findFirstByUserIdAndStatusOrderByCreatedAtAsc(String userId, SubscriptionStatus status);
}

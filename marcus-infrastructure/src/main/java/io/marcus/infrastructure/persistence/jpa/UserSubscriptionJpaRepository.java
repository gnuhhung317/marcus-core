package io.marcus.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserSubscriptionJpaRepository extends JpaRepository<JpaUserSubscriptionEntity, String> {
    List<JpaUserSubscriptionEntity> findByUserId(String userId);
}

package io.marcus.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriptionJpaRepository extends JpaRepository<JpaSubscriptionPlanEntity, String> {
    List<JpaSubscriptionPlanEntity> findByBotId(String botId);
}

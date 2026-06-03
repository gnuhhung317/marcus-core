package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotDryRunPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataBotDryRunPositionRepository extends JpaRepository<BotDryRunPositionEntity, String> {

    Optional<BotDryRunPositionEntity> findByBotIdAndPositionId(String botId, String positionId);

    List<BotDryRunPositionEntity> findByBotIdOrderByOpenedAtAsc(String botId);

    List<BotDryRunPositionEntity> findByBotIdAndStatusOrderByOpenedAtAsc(String botId, String status);
}

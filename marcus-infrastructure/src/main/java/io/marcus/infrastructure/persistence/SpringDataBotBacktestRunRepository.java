package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotBacktestRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataBotBacktestRunRepository extends JpaRepository<BotBacktestRunEntity, String> {

    Optional<BotBacktestRunEntity> findTopByBotIdOrderByCreatedAtDesc(String botId);
}

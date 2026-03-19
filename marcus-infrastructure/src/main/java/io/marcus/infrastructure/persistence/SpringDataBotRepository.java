package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpringDataBotRepository extends JpaRepository<BotEntity, String> {
    Optional<BotEntity> findByBotId(String botId);
    Optional<BotEntity> findByApiKey(String apiKey);
}

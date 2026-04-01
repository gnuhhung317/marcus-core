package io.marcus.infrastructure.persistence;

import io.marcus.domain.vo.BotStatus;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataBotRepository extends JpaRepository<BotEntity, String> {

    Optional<BotEntity> findByBotId(String botId);

    Optional<BotEntity> findByApiKey(String apiKey);

    List<BotEntity> findByStatus(BotStatus status);

    List<BotEntity> findByDeveloperId(String developerId);
}

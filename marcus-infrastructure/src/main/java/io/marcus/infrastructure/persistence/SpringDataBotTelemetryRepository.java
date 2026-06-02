package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotTelemetryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataBotTelemetryRepository extends JpaRepository<BotTelemetryEntity, String> {

    Optional<BotTelemetryEntity> findTopByBotIdOrderByTimestampDesc(String botId);

    Optional<BotTelemetryEntity> findByBotIdAndTimestamp(String botId, LocalDateTime timestamp);

    List<BotTelemetryEntity> findByBotIdOrderByTimestampAsc(String botId);

    List<BotTelemetryEntity> findByBotIdAndTimestampGreaterThanEqualOrderByTimestampAsc(String botId, LocalDateTime timestamp);
}

package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotDryRunPortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataBotDryRunPortfolioRepository extends JpaRepository<BotDryRunPortfolioEntity, String> {

    Optional<BotDryRunPortfolioEntity> findByBotIdAndTimestamp(String botId, LocalDateTime timestamp);

    Optional<BotDryRunPortfolioEntity> findTopByBotIdOrderByTimestampDesc(String botId);

    List<BotDryRunPortfolioEntity> findByBotIdOrderByTimestampAsc(String botId);
}

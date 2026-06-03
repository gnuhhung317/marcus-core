package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotDryRunClosedTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataBotDryRunClosedTradeRepository extends JpaRepository<BotDryRunClosedTradeEntity, String> {

    Optional<BotDryRunClosedTradeEntity> findByBotIdAndTradeId(String botId, String tradeId);

    List<BotDryRunClosedTradeEntity> findByBotIdOrderByExitTimestampAsc(String botId);
}

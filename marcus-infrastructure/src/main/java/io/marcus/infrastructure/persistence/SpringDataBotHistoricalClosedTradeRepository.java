package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotHistoricalClosedTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataBotHistoricalClosedTradeRepository extends JpaRepository<BotHistoricalClosedTradeEntity, String> {

    List<BotHistoricalClosedTradeEntity> findByBotIdOrderByExitTimestampAsc(String botId);

    List<BotHistoricalClosedTradeEntity> findByRunIdOrderByExitTimestampAsc(String runId);
}

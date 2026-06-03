package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotHistoricalClosedTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataBotHistoricalClosedTradeRepository extends JpaRepository<BotHistoricalClosedTradeEntity, String> {
}

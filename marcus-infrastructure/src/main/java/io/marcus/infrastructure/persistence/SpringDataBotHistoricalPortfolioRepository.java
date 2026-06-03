package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotHistoricalPortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataBotHistoricalPortfolioRepository extends JpaRepository<BotHistoricalPortfolioEntity, String> {

    List<BotHistoricalPortfolioEntity> findByRunIdOrderByTimestampAsc(String runId);
}

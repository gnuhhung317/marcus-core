package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataBotRepository extends JpaRepository<BotEntity, String> {

    @Query("select b from BotEntity b left join fetch b.exchange")
    List<BotEntity> findAllWithExchange();

    @Query("select b from BotEntity b left join fetch b.exchange where b.botId = :botId")
    Optional<BotEntity> findByBotIdWithExchange(@Param("botId") String botId);

    Optional<BotEntity> findByBotId(String botId);

    Optional<BotEntity> findByApiKey(String apiKey);
}

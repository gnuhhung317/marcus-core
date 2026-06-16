package io.marcus.infrastructure.persistence;

import io.marcus.domain.vo.BotStatus;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<BotEntity> findByStatus(BotStatus status);

    long countByStatus(BotStatus status);

    List<BotEntity> findByDeveloperId(String developerId);

    /**
     * Find all bots that are not in the specified status. Used for leaderboard
     * metrics calculation.
     */
    List<BotEntity> findByStatusNot(BotStatus status);

    @Query("""
        SELECT b FROM BotEntity b
        WHERE (:query IS NULL OR :query = '' OR
               LOWER(b.botId) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(b.tradingPair) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(b.developerId) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:status IS NULL OR b.status = :status)
          AND (:developerId IS NULL OR :developerId = '' OR b.developerId = :developerId)
        ORDER BY b.createdAt DESC
    """)
    Page<BotEntity> searchAdminBots(
            @Param("query") String query,
            @Param("status") BotStatus status,
            @Param("developerId") String developerId,
            Pageable pageable
    );
}

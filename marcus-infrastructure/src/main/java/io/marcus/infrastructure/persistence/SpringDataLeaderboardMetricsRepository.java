package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotLeaderboardMetricsEntity;
import io.marcus.infrastructure.persistence.entity.BotLeaderboardMetricsEntity.BotLeaderboardMetricsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository for bot leaderboard metrics with optimized queries for pagination
 * and sorting. Uses native SQL for efficient filtering by data_source and
 * sorting by cagr/sharpe.
 */
@Repository
public interface SpringDataLeaderboardMetricsRepository extends JpaRepository<BotLeaderboardMetricsEntity, BotLeaderboardMetricsId> {

    /**
     * Find paginated leaderboard metrics filtered by data source and sorted by
     * metric. Uses native SQL for optimal performance with large datasets.
     *
     * @param dataSource Filter by data source ('DRY_RUN' or 'HISTORICAL')
     * @param sortBy Sort field ('CAGR' or 'SHARPE')
     * @param offset Pagination offset
     * @param limit Page size
     * @return List of leaderboard metrics for the requested page
     */
    @Query(value = """
            SELECT bot_id, data_source, cagr, sharpe, max_drawdown, sample_days, 
                   last_calculated_at, created_at, updated_at
            FROM bot_leaderboard_metrics 
            WHERE data_source = :dataSource 
            ORDER BY 
                CASE WHEN :sortBy = 'CAGR' THEN cagr END DESC,
                CASE WHEN :sortBy = 'SHARPE' THEN sharpe END DESC
            OFFSET :offset LIMIT :limit
            """, nativeQuery = true)
    List<BotLeaderboardMetricsEntity> findPaginated(
            @Param("dataSource") String dataSource,
            @Param("sortBy") String sortBy,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * Count total metrics for a specific data source.
     *
     * @param dataSource Filter by data source ('DRY_RUN' or 'HISTORICAL')
     * @return Total count of metrics for the data source
     */
    @Query(value = "SELECT COUNT(*) FROM bot_leaderboard_metrics WHERE data_source = :dataSource", nativeQuery = true)
    long countByDataSource(@Param("dataSource") String dataSource);

    /**
     * Upsert leaderboard metrics for a bot and data source. Uses PostgreSQL's
     * ON CONFLICT clause for atomic insert-or-update.
     *
     * @param botId Bot identifier
     * @param dataSource Data source ('DRY_RUN' or 'HISTORICAL')
     * @param cagr Compound Annual Growth Rate
     * @param sharpe Sharpe ratio
     * @param maxDD Maximum drawdown (negative value)
     * @param days Number of days in sample period
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO bot_leaderboard_metrics (bot_id, data_source, cagr, sharpe, max_drawdown, sample_days)
            VALUES (:botId, :dataSource, :cagr, :sharpe, :maxDD, :days)
            ON CONFLICT (bot_id, data_source) DO UPDATE SET
                cagr = EXCLUDED.cagr,
                sharpe = EXCLUDED.sharpe,
                max_drawdown = EXCLUDED.max_drawdown,
                sample_days = EXCLUDED.sample_days,
                updated_at = CURRENT_TIMESTAMP,
                last_calculated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void saveOrUpdate(
            @Param("botId") String botId,
            @Param("dataSource") String dataSource,
            @Param("cagr") double cagr,
            @Param("sharpe") double sharpe,
            @Param("maxDD") double maxDD,
            @Param("days") long days
    );

    /**
     * Delete metrics for a specific bot and data source.
     *
     * @param botId Bot identifier
     * @param dataSource Data source ('DRY_RUN' or 'HISTORICAL')
     */
    @Modifying
    @Transactional
    void deleteByBotIdAndDataSource(String botId, String dataSource);
}

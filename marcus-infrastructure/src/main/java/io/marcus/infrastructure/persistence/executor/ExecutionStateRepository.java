package io.marcus.infrastructure.persistence.executor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for ExecutionStateEntity. Provides CRUD operations for
 * execution state management.
 */
@Repository
public interface ExecutionStateRepository extends JpaRepository<ExecutionStateEntity, Long> {

    /**
     * Find execution state by signal ID.
     */
    Optional<ExecutionStateEntity> findBySignalId(String signalId);

    /**
     * Check if execution state exists for signal.
     */
    boolean existsBySignalId(String signalId);

    /**
     * Find closed execution states and associated signals for a specific bot and optional asset filter.
     */
    @Query("SELECT es, s FROM ExecutionStateEntity es, SignalEntity s " +
           "WHERE es.signalId = s.signalId " +
           "AND s.botId = :botId " +
           "AND es.positionState = 'CLOSED' " +
           "AND (:asset IS NULL OR UPPER(s.symbol) LIKE UPPER(CONCAT('%', :asset, '%')))")
    List<Object[]> findClosedExecutionStatesAndSignalsForBot(
            @Param("botId") String botId,
            @Param("asset") String asset
    );
}


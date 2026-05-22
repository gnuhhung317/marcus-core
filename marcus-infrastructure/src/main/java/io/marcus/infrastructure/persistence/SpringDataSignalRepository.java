package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.SignalEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataSignalRepository extends JpaRepository<SignalEntity, String> {

    Optional<SignalEntity> findBySignalId(String signalId);

    // --- Targeted queries (replacing findAll().stream().filter() patterns) ---

    /**
     * Find all signals for a specific bot. Replaces
     * {@code findAll().stream().filter(s -> s.getBotId().equals(botId))}.
     */
    List<SignalEntity> findByBotId(String botId);

    @Query("SELECT s FROM SignalEntity s WHERE s.botId = :botId ORDER BY s.generatedTimestamp DESC NULLS LAST")
    List<SignalEntity> findByBotIdOrderByGeneratedTimestampDesc(
            @Param("botId") String botId,
            Pageable pageable
    );

    List<SignalEntity> findByBotIdAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(String botId);

    /**
     * Find signals for multiple bots with non-null timestamps, ordered for
     * time-series rendering. Replaces findAll() + filter by botIds + sort.
     */
    List<SignalEntity> findByBotIdInAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(
            List<String> botIds
    );

    /**
     * Paginated signal listing ordered by most recent first. Used by
     * {@code listSignals} endpoint to avoid loading entire signal table.
     */
    @Query("SELECT s FROM SignalEntity s ORDER BY s.generatedTimestamp DESC NULLS LAST")
    List<SignalEntity> findAllOrderByGeneratedTimestampDesc(Pageable pageable);

    /**
     * Paginated signal listing filtered by status. Used by
     * {@code listSignals} endpoint with status filter.
     */
    @Query("SELECT s FROM SignalEntity s WHERE CAST(s.status AS string) = :status " +
           "ORDER BY s.generatedTimestamp DESC NULLS LAST")
    List<SignalEntity> findByStatusStringOrderByGeneratedTimestampDesc(
            @Param("status") String status,
            Pageable pageable
    );

    // --- Decision Dashboard queries ---

    /**
     * Find signals for given bot created after specified timestamp. Used to
     * calculate win rate and success rate for decision scoring.
     *
     * @param botId bot identifier
     * @param from start timestamp (e.g., 24 hours ago)
     * @return list of signals sorted by creation date descending
     */
    List<SignalEntity> findByBotIdAndCreatedAtAfter(String botId, LocalDateTime from);
}

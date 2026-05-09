package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.SignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataSignalRepository extends JpaRepository<SignalEntity, String> {
    Optional<SignalEntity> findBySignalId(String signalId);

    // Pha 1: Decision Dashboard queries

    /**
     * Find signals for given bot created after specified timestamp.
     * Used to calculate win rate and success rate for decision scoring.
     * @param botId bot identifier
     * @param from start timestamp (e.g., 24 hours ago)
     * @return list of signals sorted by creation date descending
     */
    List<SignalEntity> findByBotIdAndCreatedAtAfter(String botId, LocalDateTime from);
}

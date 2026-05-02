package io.marcus.infrastructure.persistence.executor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

package io.marcus.domain.repository;

import io.marcus.domain.model.Signal;

public interface SignalRepository {
    /**
     * Persist signal to database.
     */
    void save(Signal signal);
    
    /**
     * Check if a signal with the given signalId already exists.
     * Used to prevent duplicate signals.
     */
    boolean existsBySignalId(String signalId);
}

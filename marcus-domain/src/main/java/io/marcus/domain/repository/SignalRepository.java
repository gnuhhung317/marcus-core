package io.marcus.domain.repository;

import io.marcus.domain.model.Signal;

import java.util.List;
import java.util.Optional;

public interface SignalRepository {

    /**
     * Persist signal to database.
     */
    void save(Signal signal);

    /**
     * Check if a signal with the given signalId already exists. Used to prevent
     * duplicate signals.
     */
    boolean existsBySignalId(String signalId);

    /**
     * Update signal lifecycle status.
     */
    void updateStatus(String signalId, io.marcus.domain.vo.SignalStatus status);

    /**
     * Find signals belonging to a specific bot, ordered by generatedTimestamp DESC.
     *
     * @param botId the bot identifier
     * @param limit max number of signals to return
     * @return list of signals for the bot
     */
    List<Signal> findByBotId(String botId, int limit);

    /**
     * Find a single signal by its unique signalId.
     *
     * @param signalId the signal identifier
     * @return the signal, or empty if not found
     */
    Optional<Signal> findBySignalId(String signalId);
}


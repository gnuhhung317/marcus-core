package io.marcus.infrastructure.adapter.executor;

import io.marcus.domain.executor.ExecutionState;
import io.marcus.domain.executor.ExecutionStatePort;
import io.marcus.infrastructure.persistence.executor.ExecutionStateEntity;
import io.marcus.infrastructure.persistence.executor.ExecutionStateRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Infrastructure adapter for ExecutionStatePort. Manages execution state per
 * signal using Spring Data JPA.
 */
@Component
public class ExecutionStateAdapter implements ExecutionStatePort {

    private final ExecutionStateRepository executionStateRepository;

    public ExecutionStateAdapter(ExecutionStateRepository executionStateRepository) {
        this.executionStateRepository = executionStateRepository;
    }

    @Override
    public Optional<ExecutionState> getState(String signalId) {
        return executionStateRepository.findBySignalId(signalId)
                .map(this::mapToDomain);
    }

    @Override
    public ExecutionState acceptSignal(String signalId) {
        if (executionStateRepository.existsBySignalId(signalId)) {
            throw new IllegalArgumentException("Signal already has a state: " + signalId);
        }

        ExecutionStateEntity entity = new ExecutionStateEntity(
                signalId,
                "ACCEPTED",
                "NONE",
                "NONE",
                -1,
                null,
                null
        );

        ExecutionStateEntity saved = executionStateRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public ExecutionState rejectSignal(String signalId) {
        if (executionStateRepository.existsBySignalId(signalId)) {
            throw new IllegalArgumentException("Signal already has a state: " + signalId);
        }

        Instant now = Instant.now();
        ExecutionStateEntity entity = new ExecutionStateEntity(
                signalId,
                "REJECTED",
                "NONE",
                "NONE",
                0,
                now,
                now
        );

        ExecutionStateEntity saved = executionStateRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public ExecutionState updateOrderPlaced(String signalId, int newLastSequence, Instant sentAt) {
        ExecutionStateEntity entity = executionStateRepository.findBySignalId(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal state not found: " + signalId));

        entity.setOrderState("PLACED");
        entity.setLastSequence(newLastSequence);
        entity.setLastEventTime(sentAt);

        ExecutionStateEntity saved = executionStateRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public ExecutionState updateOrderFilled(String signalId, int newLastSequence, Instant sentAt) {
        ExecutionStateEntity entity = executionStateRepository.findBySignalId(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal state not found: " + signalId));

        entity.setOrderState("FILLED");
        entity.setSignalState("OPEN");
        entity.setLastSequence(newLastSequence);
        entity.setLastEventTime(sentAt);

        ExecutionStateEntity saved = executionStateRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public ExecutionState updateOrderFailed(String signalId, int newLastSequence, Instant sentAt) {
        ExecutionStateEntity entity = executionStateRepository.findBySignalId(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal state not found: " + signalId));

        entity.setOrderState("FAILED");
        entity.setLastSequence(newLastSequence);
        entity.setLastEventTime(sentAt);
        entity.setSignalState("CLOSED");
        entity.setClosedAt(sentAt);

        ExecutionStateEntity saved = executionStateRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public ExecutionState updateOrderCanceled(String signalId, int newLastSequence, Instant sentAt) {
        ExecutionStateEntity entity = executionStateRepository.findBySignalId(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal state not found: " + signalId));

        entity.setOrderState("CANCELED");
        entity.setLastSequence(newLastSequence);
        entity.setLastEventTime(sentAt);
        entity.setSignalState("CLOSED");
        entity.setClosedAt(sentAt);

        ExecutionStateEntity saved = executionStateRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public ExecutionState updatePositionOpened(String signalId, int newLastSequence, Instant sentAt) {
        ExecutionStateEntity entity = executionStateRepository.findBySignalId(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal state not found: " + signalId));

        entity.setPositionState("OPENED");
        entity.setLastSequence(newLastSequence);
        entity.setLastEventTime(sentAt);

        ExecutionStateEntity saved = executionStateRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public ExecutionState updatePositionUpdated(String signalId, int newLastSequence, Instant sentAt) {
        ExecutionStateEntity entity = executionStateRepository.findBySignalId(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal state not found: " + signalId));

        if (!"UPDATING".equals(entity.getPositionState())) {
            entity.setPositionState("UPDATING");
        }
        entity.setLastSequence(newLastSequence);
        entity.setLastEventTime(sentAt);

        ExecutionStateEntity saved = executionStateRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public ExecutionState updatePositionClosed(String signalId, int newLastSequence, Instant sentAt) {
        ExecutionStateEntity entity = executionStateRepository.findBySignalId(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal state not found: " + signalId));

        entity.setPositionState("CLOSED");
        entity.setSignalState("CLOSED");
        entity.setLastSequence(newLastSequence);
        entity.setLastEventTime(sentAt);
        entity.setClosedAt(sentAt);

        ExecutionStateEntity saved = executionStateRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public boolean isPositionClosed(String signalId) {
        Optional<ExecutionStateEntity> entity = executionStateRepository.findBySignalId(signalId);
        return entity.isPresent() && "CLOSED".equals(entity.get().getPositionState());
    }

    @Override
    public int getLastSequence(String signalId) {
        return executionStateRepository.findBySignalId(signalId)
                .map(ExecutionStateEntity::getLastSequence)
                .orElse(-1);
    }

    /**
     * Map JPA entity to domain object.
     */
    private ExecutionState mapToDomain(ExecutionStateEntity entity) {
        return new ExecutionState(
                entity.getSignalId(),
                ExecutionState.SignalState.valueOf(entity.getSignalState()),
                ExecutionState.OrderState.valueOf(entity.getOrderState()),
                ExecutionState.PositionState.valueOf(entity.getPositionState()),
                entity.getLastSequence(),
                entity.getLastEventTime(),
                entity.getClosedAt()
        );
    }
}

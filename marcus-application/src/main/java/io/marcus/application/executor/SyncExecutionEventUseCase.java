package io.marcus.application.executor;

import io.marcus.domain.executor.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Application use case for synchronizing execution events from the executor
 * client.
 *
 * Responsibilities: - Validate incoming events (sequence, state transitions,
 * duplicates) - Enforce late event rejection (no events after position.closed)
 * - Persist events via ExecutionEventPort - Update execution state via
 * ExecutionStatePort - Return ACK response for client
 *
 * Implements Domain-First Clean Modular Monolith pattern: - Depends on domain
 * ports (ExecutionEventPort, ExecutionStatePort) - Does NOT depend on specific
 * infrastructure implementations - Orchestrates use case flow; domain models do
 * the validation
 */
@Service
@RequiredArgsConstructor
public class SyncExecutionEventUseCase {

    private final ExecutionEventPort executionEventPort;
    private final ExecutionStatePort executionStatePort;
    private final ConcurrentMap<String, ReentrantLock> signalLocks = new ConcurrentHashMap<>();

    /**
     * Process an incoming execution event from the executor client.
     *
     * @param input the incoming event
     * @return ACK response (OK or ERROR)
     */
    @Transactional
    public SyncExecutionEventOutput execute(SyncExecutionEventInput input) {
        ReentrantLock signalLock = signalLocks.computeIfAbsent(input.getSignalId(), ignored -> new ReentrantLock());
        signalLock.lock();
        try {
            validateEventInput(input);

            // Step 1: Check for duplicate (idempotency) and repair stale state from persisted events
            if (executionEventPort.findByEventId(input.getEventId()).isPresent()) {
                reconcilePersistedState(input.getSignalId());
                return SyncExecutionEventOutput.ok(
                        input.getEventId(),
                        input.getSignalId(),
                        Instant.now()
                );
            }

            // Step 3: Get current execution state
            Optional<ExecutionState> currentStateOpt = loadCurrentStateWithRepair(input.getSignalId());

            // Step 4: Check for late events (after position.closed)
            if (currentStateOpt.isPresent() && currentStateOpt.get().isPositionClosed()) {
                return SyncExecutionEventOutput.error(
                        input.getEventId(),
                        input.getSignalId(),
                        "POSITION_CLOSED",
                        "Cannot accept event for closed position on signal " + input.getSignalId(),
                        Instant.now()
                );
            }

            // Step 5: Check sequence ordering
            int expectedSequence = currentStateOpt.map(ExecutionState::getLastSequence)
                    .map(s -> s + 1)
                    .orElse(0);

            if (input.getSequence() != expectedSequence) {
                return SyncExecutionEventOutput.error(
                        input.getEventId(),
                        input.getSignalId(),
                        "OUT_OF_ORDER",
                        "Expected sequence " + expectedSequence + ", received " + input.getSequence()
                        + " for signalId " + input.getSignalId(),
                        Instant.now()
                );
            }

            // Step 6: Validate state transitions (domain logic)
            ExecutionEventType eventType = ExecutionEventType.fromCode(input.getEventType());
            validateStateTransition(eventType, currentStateOpt);

            // Step 7: Create domain event and persist
            ExecutionEvent event = ExecutionEvent.create(
                    input.getEventId(),
                    input.getSignalId(),
                    input.getSequence(),
                    eventType,
                    input.getSentAt(),
                    input.getExchangeTime(),
                    input.getPayload()
            );

            executionEventPort.store(event);

            // Step 8: Update execution state based on event type
            updateExecutionState(input.getSignalId(), eventType, input.getSequence(), input.getSentAt());

            // Step 9: Return success ACK
            return SyncExecutionEventOutput.ok(
                    input.getEventId(),
                    input.getSignalId(),
                    Instant.now()
            );

        } catch (IllegalArgumentException e) {
            return SyncExecutionEventOutput.error(
                    input.getEventId(),
                    input.getSignalId(),
                    "INVALID_STATE",
                    e.getMessage(),
                    Instant.now()
            );
        } catch (Exception e) {
            return SyncExecutionEventOutput.error(
                    input.getEventId(),
                    input.getSignalId(),
                    "INTERNAL_ERROR",
                    "Backend error: " + e.getMessage(),
                    Instant.now()
            );
        } finally {
            signalLock.unlock();
            if (!signalLock.hasQueuedThreads()) {
                signalLocks.remove(input.getSignalId(), signalLock);
            }
        }
    }

    /**
     * Validate the structure of the incoming event.
     */
    private void validateEventInput(SyncExecutionEventInput input) {
        if (input.getEventId() == null || input.getEventId().trim().isEmpty()) {
            throw new IllegalArgumentException("eventId must not be empty");
        }

        if (input.getSignalId() == null || input.getSignalId().trim().isEmpty()) {
            throw new IllegalArgumentException("signalId must not be empty");
        }

        if (input.getSequence() < 0) {
            throw new IllegalArgumentException("sequence must be >= 0");
        }

        if (input.getEventType() == null || input.getEventType().trim().isEmpty()) {
            throw new IllegalArgumentException("eventType must not be empty");
        }

        if (input.getSentAt() == null) {
            throw new IllegalArgumentException("sentAt must not be null");
        }

        // Validate eventType is recognized
        try {
            ExecutionEventType.fromCode(input.getEventType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown eventType: " + input.getEventType());
        }
    }

    private Optional<ExecutionState> loadCurrentStateWithRepair(String signalId) {
        Optional<ExecutionState> currentStateOpt = executionStatePort.getState(signalId);
        List<ExecutionEvent> persistedEvents = executionEventPort.findBySignalIdOrderBySequence(signalId);
        if (persistedEvents.isEmpty()) {
            return currentStateOpt;
        }

        ExecutionState rebuiltState = rebuildState(signalId, persistedEvents);
        if (currentStateOpt.isEmpty() || !statesEqual(currentStateOpt.get(), rebuiltState)) {
            executionStatePort.upsertState(rebuiltState);
            return Optional.of(rebuiltState);
        }

        return currentStateOpt;
    }

    private void reconcilePersistedState(String signalId) {
        List<ExecutionEvent> persistedEvents = executionEventPort.findBySignalIdOrderBySequence(signalId);
        if (persistedEvents.isEmpty()) {
            return;
        }
        executionStatePort.upsertState(rebuildState(signalId, persistedEvents));
    }

    private ExecutionState rebuildState(String signalId, List<ExecutionEvent> persistedEvents) {
        ExecutionState.SignalState signalState = ExecutionState.SignalState.ACCEPTED;
        ExecutionState.OrderState orderState = ExecutionState.OrderState.NONE;
        ExecutionState.PositionState positionState = ExecutionState.PositionState.NONE;
        int lastSequence = -1;
        Instant lastEventTime = null;
        Instant closedAt = null;

        for (ExecutionEvent event : persistedEvents) {
            lastSequence = event.getSequence();
            lastEventTime = event.getSentAt();

            switch (event.getEventType()) {
                case SIGNAL_ACCEPTED:
                    signalState = ExecutionState.SignalState.ACCEPTED;
                    orderState = ExecutionState.OrderState.NONE;
                    positionState = ExecutionState.PositionState.NONE;
                    closedAt = null;
                    break;
                case SIGNAL_REJECTED:
                    signalState = ExecutionState.SignalState.REJECTED;
                    orderState = ExecutionState.OrderState.NONE;
                    positionState = ExecutionState.PositionState.NONE;
                    closedAt = event.getSentAt();
                    break;
                case ORDER_PLACED:
                    signalState = ExecutionState.SignalState.ACCEPTED;
                    orderState = ExecutionState.OrderState.PLACED;
                    closedAt = null;
                    break;
                case ORDER_FILLED:
                    signalState = ExecutionState.SignalState.OPEN;
                    orderState = ExecutionState.OrderState.FILLED;
                    closedAt = null;
                    break;
                case ORDER_FAILED:
                    signalState = ExecutionState.SignalState.CLOSED;
                    orderState = ExecutionState.OrderState.FAILED;
                    closedAt = event.getSentAt();
                    break;
                case ORDER_CANCELED:
                    signalState = ExecutionState.SignalState.CLOSED;
                    orderState = ExecutionState.OrderState.CANCELED;
                    closedAt = event.getSentAt();
                    break;
                case POSITION_OPENED:
                    signalState = ExecutionState.SignalState.OPEN;
                    positionState = ExecutionState.PositionState.OPENED;
                    closedAt = null;
                    break;
                case POSITION_UPDATED:
                    signalState = ExecutionState.SignalState.OPEN;
                    positionState = ExecutionState.PositionState.UPDATING;
                    closedAt = null;
                    break;
                case POSITION_CLOSED:
                    signalState = ExecutionState.SignalState.CLOSED;
                    positionState = ExecutionState.PositionState.CLOSED;
                    closedAt = event.getSentAt();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
            }
        }

        return new ExecutionState(
                signalId,
                signalState,
                orderState,
                positionState,
                lastSequence,
                lastEventTime,
                closedAt
        );
    }

    private boolean statesEqual(ExecutionState left, ExecutionState right) {
        return left.getSignalState() == right.getSignalState()
                && left.getOrderState() == right.getOrderState()
                && left.getPositionState() == right.getPositionState()
                && left.getLastSequence() == right.getLastSequence()
                && java.util.Objects.equals(left.getLastEventTime(), right.getLastEventTime())
                && java.util.Objects.equals(left.getClosedAt(), right.getClosedAt());
    }

    /**
     * Validate that the event type is allowed in the current state (state
     * machine rules).
     */
    private void validateStateTransition(ExecutionEventType eventType, Optional<ExecutionState> currentStateOpt) {
        if (!currentStateOpt.isPresent()) {
            // First event must be signal.accepted
            if (eventType != ExecutionEventType.SIGNAL_ACCEPTED) {
                throw new IllegalArgumentException(
                        "First event for signal must be signal.accepted, got " + eventType
                );
            }
            return;
        }

        ExecutionState currentState = currentStateOpt.get();

        // After position closed, no events allowed
        if (currentState.isPositionClosed()) {
            throw new IllegalArgumentException(
                    "Cannot accept event for closed position"
            );
        }

        // Validate transitions based on state machine (see executor-event-state-machine.md)
        switch (eventType) {
            case SIGNAL_ACCEPTED:
                throw new IllegalArgumentException("signal.accepted can only be first event");

            case SIGNAL_REJECTED:
                if (currentState.getSignalState() != ExecutionState.SignalState.ACCEPTED) {
                    throw new IllegalArgumentException(
                            "signal.rejected only allowed after signal.accepted"
                    );
                }
                break;

            case ORDER_PLACED:
                if (currentState.getSignalState() != ExecutionState.SignalState.ACCEPTED) {
                    throw new IllegalArgumentException(
                            "order.placed only allowed when signal is ACCEPTED"
                    );
                }
                break;

            case ORDER_FILLED:
                if (currentState.getOrderState() != ExecutionState.OrderState.PLACED) {
                    throw new IllegalArgumentException(
                            "order.filled only allowed when order is PLACED"
                    );
                }
                break;

            case ORDER_FAILED:
            case ORDER_CANCELED:
                if (currentState.getOrderState() != ExecutionState.OrderState.PLACED) {
                    throw new IllegalArgumentException(
                            eventType + " only allowed when order is PLACED"
                    );
                }
                break;

            case POSITION_OPENED:
                if (currentState.getOrderState() != ExecutionState.OrderState.FILLED) {
                    throw new IllegalArgumentException(
                            "position.opened only allowed after order.filled"
                    );
                }
                break;

            case POSITION_UPDATED:
                if (currentState.getPositionState() != ExecutionState.PositionState.OPENED
                        && currentState.getPositionState() != ExecutionState.PositionState.UPDATING) {
                    throw new IllegalArgumentException(
                            "position.updated only allowed when position is OPENED or UPDATING"
                    );
                }
                break;

            case POSITION_CLOSED:
                if (currentState.getPositionState() != ExecutionState.PositionState.OPENED
                        && currentState.getPositionState() != ExecutionState.PositionState.UPDATING) {
                    throw new IllegalArgumentException(
                            "position.closed only allowed when position is OPENED or UPDATING"
                    );
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }

    /**
     * Update execution state based on the accepted event.
     */
    private void updateExecutionState(
            String signalId,
            ExecutionEventType eventType,
            int sequence,
            Instant sentAt
    ) {
        switch (eventType) {
            case SIGNAL_ACCEPTED:
                executionStatePort.acceptSignal(signalId, sequence, sentAt);
                break;

            case SIGNAL_REJECTED:
                executionStatePort.rejectSignal(signalId);
                break;

            case ORDER_PLACED:
                executionStatePort.updateOrderPlaced(signalId, sequence, sentAt);
                break;

            case ORDER_FILLED:
                executionStatePort.updateOrderFilled(signalId, sequence, sentAt);
                break;

            case ORDER_FAILED:
                executionStatePort.updateOrderFailed(signalId, sequence, sentAt);
                break;

            case ORDER_CANCELED:
                executionStatePort.updateOrderCanceled(signalId, sequence, sentAt);
                break;

            case POSITION_OPENED:
                executionStatePort.updatePositionOpened(signalId, sequence, sentAt);
                break;

            case POSITION_UPDATED:
                executionStatePort.updatePositionUpdated(signalId, sequence, sentAt);
                break;

            case POSITION_CLOSED:
                executionStatePort.updatePositionClosed(signalId, sequence, sentAt);
                break;

            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }
}

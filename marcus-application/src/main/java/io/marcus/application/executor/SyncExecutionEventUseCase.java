package io.marcus.application.executor;

import io.marcus.domain.executor.*;
import java.time.Instant;
import java.util.Optional;

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
public class SyncExecutionEventUseCase {

    private final ExecutionEventPort executionEventPort;
    private final ExecutionStatePort executionStatePort;

    public SyncExecutionEventUseCase(
            ExecutionEventPort executionEventPort,
            ExecutionStatePort executionStatePort
    ) {
        this.executionEventPort = executionEventPort;
        this.executionStatePort = executionStatePort;
    }

    /**
     * Process an incoming execution event from the executor client.
     *
     * @param input the incoming event
     * @return ACK response (OK or ERROR)
     */
    public SyncExecutionEventOutput execute(SyncExecutionEventInput input) {
        try {
            // Step 1: Validate event structure
            validateEventInput(input);

            // Step 2: Check for duplicate (idempotency)
            if (executionEventPort.existsByEventId(input.getEventId())) {
                return SyncExecutionEventOutput.ok(
                        input.getEventId(),
                        input.getSignalId(),
                        Instant.now()
                );
            }

            // Step 3: Get current execution state
            Optional<ExecutionState> currentStateOpt = executionStatePort.getState(input.getSignalId());

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
                executionStatePort.acceptSignal(signalId);
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

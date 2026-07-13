package io.marcus.application.executor;

import io.marcus.domain.executor.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SyncExecutionEventUseCase.
 */
class SyncExecutionEventUseCaseTest {

    @Mock
    private ExecutionEventPort executionEventPort;

    @Mock
    private ExecutionStatePort executionStatePort;

    private SyncExecutionEventUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new SyncExecutionEventUseCase(executionEventPort, executionStatePort);
    }

    @Test
    void testExecuteSignalAcceptedSuccessfully() {
        String eventId = "evt-001";
        String signalId = "sig-001";
        Instant now = Instant.now();

        SyncExecutionEventInput input = new SyncExecutionEventInput(
                eventId, signalId, 0, "signal.accepted", now, null,
                new HashMap<>(Map.of("riskLevel", "medium"))
        );

        when(executionEventPort.findByEventId(eventId)).thenReturn(Optional.empty());
        when(executionEventPort.findBySignalIdOrderBySequence(signalId)).thenReturn(java.util.List.of());
        when(executionStatePort.getState(signalId)).thenReturn(Optional.empty());
        when(executionStatePort.acceptSignal(signalId, 0, now))
                .thenReturn(ExecutionState.accepted(signalId));

        SyncExecutionEventOutput output = useCase.execute(input);

        assertTrue(output.isSuccess());
        assertEquals("OK", output.getStatus());
        assertNull(output.getErrorCode());
        verify(executionEventPort, times(1)).store(any(ExecutionEvent.class));
        verify(executionStatePort, times(1)).acceptSignal(signalId, 0, now);
    }

    @Test
    void testExecuteOrderPlacedAfterSignalAcceptedSuccessfully() {
        String eventId = "evt-002";
        String signalId = "sig-001";
        Instant now = Instant.now();

        SyncExecutionEventInput input = new SyncExecutionEventInput(
                eventId, signalId, 1, "order.placed", now, null,
                new HashMap<>(Map.of("order_id", "order-001"))
        );

        ExecutionState currentState = new ExecutionState(
                signalId,
                ExecutionState.SignalState.ACCEPTED,
                ExecutionState.OrderState.NONE,
                ExecutionState.PositionState.NONE,
                0,
                now,
                null
        );

        when(executionEventPort.findByEventId(eventId)).thenReturn(Optional.empty());
        when(executionEventPort.findBySignalIdOrderBySequence(signalId)).thenReturn(java.util.List.of());
        when(executionStatePort.getState(signalId)).thenReturn(Optional.of(currentState));
        when(executionStatePort.updateOrderPlaced(signalId, 1, now)).thenReturn(currentState);

        SyncExecutionEventOutput output = useCase.execute(input);

        assertTrue(output.isSuccess());
        assertEquals("OK", output.getStatus());
        verify(executionEventPort, times(1)).store(any(ExecutionEvent.class));
        verify(executionStatePort, times(1)).updateOrderPlaced(signalId, 1, now);
    }

    @Test
    void testExecuteDuplicateEventRepairsPersistedState() {
        String eventId = "evt-001";
        String signalId = "sig-001";
        Instant now = Instant.now();

        SyncExecutionEventInput input = new SyncExecutionEventInput(
                eventId, signalId, 0, "signal.accepted", now, null,
                new HashMap<>()
        );

        ExecutionEvent storedEvent = ExecutionEvent.create(
                eventId,
                signalId,
                0,
                ExecutionEventType.SIGNAL_ACCEPTED,
                now,
                null,
                new HashMap<>()
        );
        when(executionEventPort.findByEventId(eventId)).thenReturn(Optional.of(storedEvent));
        when(executionEventPort.findBySignalIdOrderBySequence(signalId)).thenReturn(java.util.List.of(storedEvent));
        when(executionStatePort.upsertState(any(ExecutionState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SyncExecutionEventOutput output = useCase.execute(input);

        assertTrue(output.isSuccess());
        assertEquals("OK", output.getStatus());
        verify(executionEventPort, never()).store(any(ExecutionEvent.class));
        verify(executionStatePort).upsertState(argThat(state ->
                state.getSignalId().equals(signalId)
                        && state.getLastSequence() == 0
                        && state.getSignalState() == ExecutionState.SignalState.ACCEPTED
        ));
    }

    @Test
    void testExecuteOutOfOrderEventRejected() {
        String eventId = "evt-002";
        String signalId = "sig-001";
        Instant now = Instant.now();

        SyncExecutionEventInput input = new SyncExecutionEventInput(
                eventId, signalId, 5, "order.placed", now, null,
                new HashMap<>()
        );

        ExecutionState currentState = ExecutionState.accepted(signalId);
        when(executionEventPort.findByEventId(eventId)).thenReturn(Optional.empty());
        when(executionEventPort.findBySignalIdOrderBySequence(signalId)).thenReturn(java.util.List.of());
        when(executionStatePort.getState(signalId)).thenReturn(Optional.of(currentState));

        SyncExecutionEventOutput output = useCase.execute(input);

        assertFalse(output.isSuccess());
        assertEquals("ERROR", output.getStatus());
        assertEquals("OUT_OF_ORDER", output.getErrorCode());
        assertTrue(output.getErrorMessage().contains("Expected sequence 0, received 5"));
        verify(executionEventPort, never()).store(any(ExecutionEvent.class));
    }

    @Test
    void testExecuteLateEventAfterPositionClosedRejected() {
        String eventId = "evt-006";
        String signalId = "sig-001";
        Instant now = Instant.now();

        SyncExecutionEventInput input = new SyncExecutionEventInput(
                eventId, signalId, 6, "order.placed", now, null,
                new HashMap<>()
        );

        ExecutionState closedState = new ExecutionState(
                signalId,
                ExecutionState.SignalState.CLOSED,
                ExecutionState.OrderState.FILLED,
                ExecutionState.PositionState.CLOSED,
                5,
                now,
                now
        );

        when(executionEventPort.findByEventId(eventId)).thenReturn(Optional.empty());
        when(executionEventPort.findBySignalIdOrderBySequence(signalId)).thenReturn(java.util.List.of());
        when(executionStatePort.getState(signalId)).thenReturn(Optional.of(closedState));

        SyncExecutionEventOutput output = useCase.execute(input);

        assertFalse(output.isSuccess());
        assertEquals("ERROR", output.getStatus());
        assertEquals("POSITION_CLOSED", output.getErrorCode());
        assertTrue(output.getErrorMessage().contains("Cannot accept event for closed position"));
        verify(executionEventPort, never()).store(any(ExecutionEvent.class));
    }

    @Test
    void testExecuteValidatesUnknownEventType() {
        Instant now = Instant.now();

        SyncExecutionEventInput input = new SyncExecutionEventInput(
                "evt-001", "sig-001", 0, "unknown.event", now, null,
                new HashMap<>()
        );

        SyncExecutionEventOutput output = useCase.execute(input);

        assertFalse(output.isSuccess());
        assertEquals("INVALID_STATE", output.getErrorCode());
        assertTrue(output.getErrorMessage().contains("Unknown eventType"));
    }

    @Test
    void testExecuteRepairsStaleStateBeforeAcceptingNextSequence() {
        String signalId = "sig-001";
        Instant acceptedAt = Instant.now();
        Instant placedAt = acceptedAt.plusSeconds(5);
        ExecutionEvent acceptedEvent = ExecutionEvent.create(
                "evt-accepted",
                signalId,
                0,
                ExecutionEventType.SIGNAL_ACCEPTED,
                acceptedAt,
                null,
                new HashMap<>()
        );
        ExecutionState staleState = ExecutionState.accepted(signalId);
        ExecutionState repairedState = new ExecutionState(
                signalId,
                ExecutionState.SignalState.ACCEPTED,
                ExecutionState.OrderState.NONE,
                ExecutionState.PositionState.NONE,
                0,
                acceptedAt,
                null
        );
        SyncExecutionEventInput input = new SyncExecutionEventInput(
                "evt-placed", signalId, 1, "order.placed", placedAt, null,
                new HashMap<>(Map.of("order_id", "order-001"))
        );

        when(executionEventPort.findByEventId("evt-placed")).thenReturn(Optional.empty());
        when(executionEventPort.findBySignalIdOrderBySequence(signalId)).thenReturn(java.util.List.of(acceptedEvent));
        when(executionStatePort.getState(signalId)).thenReturn(Optional.of(staleState));
        when(executionStatePort.upsertState(any(ExecutionState.class))).thenReturn(repairedState);
        when(executionStatePort.updateOrderPlaced(signalId, 1, placedAt)).thenReturn(repairedState);

        SyncExecutionEventOutput output = useCase.execute(input);

        assertTrue(output.isSuccess());
        verify(executionStatePort).upsertState(argThat(state -> state.getLastSequence() == 0));
        verify(executionStatePort).updateOrderPlaced(signalId, 1, placedAt);
    }
}

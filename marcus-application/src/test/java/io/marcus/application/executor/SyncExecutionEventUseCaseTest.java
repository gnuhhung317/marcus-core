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

        when(executionEventPort.existsByEventId(eventId)).thenReturn(false);
        when(executionStatePort.getState(signalId)).thenReturn(Optional.empty());
        when(executionStatePort.acceptSignal(signalId))
                .thenReturn(ExecutionState.accepted(signalId));

        SyncExecutionEventOutput output = useCase.execute(input);

        assertTrue(output.isSuccess());
        assertEquals("OK", output.getStatus());
        assertNull(output.getErrorCode());
        verify(executionEventPort, times(1)).store(any(ExecutionEvent.class));
        verify(executionStatePort, times(1)).acceptSignal(signalId);
    }

    @Test
    void testExecuteDuplicateEventIdempotent() {
        String eventId = "evt-001";
        String signalId = "sig-001";
        Instant now = Instant.now();

        SyncExecutionEventInput input = new SyncExecutionEventInput(
                eventId, signalId, 0, "signal.accepted", now, null,
                new HashMap<>()
        );

        // Simulate duplicate: event already exists
        when(executionEventPort.existsByEventId(eventId)).thenReturn(true);

        SyncExecutionEventOutput output = useCase.execute(input);

        // Should return OK without reprocessing
        assertTrue(output.isSuccess());
        assertEquals("OK", output.getStatus());

        // Should NOT store the event again
        verify(executionEventPort, never()).store(any(ExecutionEvent.class));
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
        when(executionEventPort.existsByEventId(eventId)).thenReturn(false);
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

        when(executionEventPort.existsByEventId(eventId)).thenReturn(false);
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
}

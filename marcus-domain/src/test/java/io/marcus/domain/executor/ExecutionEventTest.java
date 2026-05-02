package io.marcus.domain.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExecutionEvent and ExecutionState domain models.
 */
class ExecutionEventTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void testExecutionEventCreate() {
        String eventId = "550e8400-e29b-41d4-a716-446655440000";
        String signalId = "660e8400-e29b-41d4-a716-446655440001";
        int sequence = 1;
        ExecutionEventType eventType = ExecutionEventType.ORDER_PLACED;
        Instant sentAt = Instant.now();
        Instant exchangeTime = Instant.now().minusSeconds(1);
        Object payload = new HashMap<>(Map.of("orderId", "ORD-123"));

        ExecutionEvent event = ExecutionEvent.create(
                eventId, signalId, sequence, eventType, sentAt, exchangeTime, payload
        );

        assertNotNull(event);
        assertEquals(eventId, event.getEventId());
        assertEquals(signalId, event.getSignalId());
        assertEquals(sequence, event.getSequence());
        assertEquals(eventType, event.getEventType());
        assertEquals(sentAt, event.getSentAt());
        assertEquals(exchangeTime, event.getExchangeTime());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void testExecutionEventNullValidation() {
        assertThrows(NullPointerException.class, ()
                -> ExecutionEvent.create(null, "sig-1", 0, ExecutionEventType.SIGNAL_ACCEPTED, Instant.now(), null, new HashMap<>())
        );

        assertThrows(NullPointerException.class, ()
                -> ExecutionEvent.create("evt-1", null, 0, ExecutionEventType.SIGNAL_ACCEPTED, Instant.now(), null, new HashMap<>())
        );

        assertThrows(NullPointerException.class, ()
                -> ExecutionEvent.create("evt-1", "sig-1", 0, null, Instant.now(), null, new HashMap<>())
        );

        assertThrows(NullPointerException.class, ()
                -> ExecutionEvent.create("evt-1", "sig-1", 0, ExecutionEventType.SIGNAL_ACCEPTED, null, null, new HashMap<>())
        );
    }

    @Test
    void testExecutionEventSequenceValidation() {
        assertThrows(IllegalArgumentException.class, ()
                -> ExecutionEvent.create("evt-1", "sig-1", -1, ExecutionEventType.SIGNAL_ACCEPTED, Instant.now(), null, new HashMap<>())
        );
    }

    @Test
    void testExecutionEventEquality() {
        String eventId = "550e8400-e29b-41d4-a716-446655440000";
        String signalId = "660e8400-e29b-41d4-a716-446655440001";

        ExecutionEvent event1 = ExecutionEvent.create(
                eventId, signalId, 1, ExecutionEventType.ORDER_PLACED,
                Instant.now(), null, new HashMap<>()
        );

        ExecutionEvent event2 = ExecutionEvent.create(
                eventId, signalId, 1, ExecutionEventType.ORDER_PLACED,
                Instant.now(), null, new HashMap<>()
        );

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testExecutionEventPositionClosingDetection() {
        String eventId = "550e8400-e29b-41d4-a716-446655440000";
        String signalId = "660e8400-e29b-41d4-a716-446655440001";

        ExecutionEvent closeEvent = ExecutionEvent.create(
                eventId, signalId, 5, ExecutionEventType.POSITION_CLOSED,
                Instant.now(), null, new HashMap<>()
        );

        ExecutionEvent rejectEvent = ExecutionEvent.create(
                eventId, signalId, 0, ExecutionEventType.SIGNAL_REJECTED,
                Instant.now(), null, new HashMap<>()
        );

        ExecutionEvent normalEvent = ExecutionEvent.create(
                eventId, signalId, 1, ExecutionEventType.ORDER_PLACED,
                Instant.now(), null, new HashMap<>()
        );

        assertTrue(closeEvent.isPositionClosing());
        assertTrue(rejectEvent.isPositionClosing());
        assertFalse(normalEvent.isPositionClosing());
    }
}

/**
 * Unit tests for ExecutionState aggregate.
 */
class ExecutionStateTest {

    @Test
    void testExecutionStateAccepted() {
        String signalId = "660e8400-e29b-41d4-a716-446655440001";

        ExecutionState state = ExecutionState.accepted(signalId);

        assertEquals(signalId, state.getSignalId());
        assertEquals(ExecutionState.SignalState.ACCEPTED, state.getSignalState());
        assertEquals(ExecutionState.OrderState.NONE, state.getOrderState());
        assertEquals(ExecutionState.PositionState.NONE, state.getPositionState());
        assertEquals(-1, state.getLastSequence());
        assertNull(state.getLastEventTime());
        assertNull(state.getClosedAt());
        assertTrue(state.allowsFurtherEvents());
        assertFalse(state.isPositionClosed());
    }

    @Test
    void testExecutionStateRejected() {
        String signalId = "660e8400-e29b-41d4-a716-446655440001";
        Instant rejectedAt = Instant.now();

        ExecutionState state = ExecutionState.rejected(signalId, rejectedAt);

        assertEquals(signalId, state.getSignalId());
        assertEquals(ExecutionState.SignalState.REJECTED, state.getSignalState());
        assertEquals(0, state.getLastSequence());
        assertEquals(rejectedAt, state.getClosedAt());
        assertFalse(state.allowsFurtherEvents());
    }

    @Test
    void testExecutionStatePositionClosed() {
        String signalId = "660e8400-e29b-41d4-a716-446655440001";

        ExecutionState state = new ExecutionState(
                signalId,
                ExecutionState.SignalState.CLOSED,
                ExecutionState.OrderState.FILLED,
                ExecutionState.PositionState.CLOSED,
                5,
                Instant.now(),
                Instant.now()
        );

        assertTrue(state.isPositionClosed());
        assertFalse(state.allowsFurtherEvents());
    }
}

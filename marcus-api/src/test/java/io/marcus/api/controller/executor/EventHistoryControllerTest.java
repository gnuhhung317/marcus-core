package io.marcus.api.controller.executor;

import io.marcus.domain.executor.ExecutionEvent;
import io.marcus.domain.executor.ExecutionEventPort;
import io.marcus.domain.executor.ExecutionEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for EventHistoryController.
 */
class EventHistoryControllerTest {

    @Mock
    private ExecutionEventPort executionEventPort;

    private EventHistoryController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new EventHistoryController(executionEventPort);
    }

    @Test
    void testQueryEventsSuccessfully() {
        String signalId = "sig-001";
        Instant now = Instant.now();

        // Create mock events
        List<ExecutionEvent> mockEvents = new ArrayList<>();
        mockEvents.add(new ExecutionEvent(
                "evt-001", signalId, 0, ExecutionEventType.SIGNAL_ACCEPTED, now, null, new HashMap<>(), now
        ));
        mockEvents.add(new ExecutionEvent(
                "evt-002", signalId, 1, ExecutionEventType.ORDER_PLACED, now.plusSeconds(1), null, new HashMap<>(), now.plusSeconds(1)
        ));

        // Mock port responses
        when(executionEventPort.countBySignalId(signalId)).thenReturn(10L);
        when(executionEventPort.findBySignalIdAndSequenceRange(signalId, 0, 49))
                .thenReturn(mockEvents);

        // Execute
        ResponseEntity<EventHistoryResponse> response = controller.queryEvents(signalId, 0, 50);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(signalId, response.getBody().getSignalId());
        assertEquals(2, response.getBody().getEvents().size());
        assertEquals(10, response.getBody().getTotalCount());
        assertTrue(response.getBody().isHasMore());

        // Verify events
        EventHistoryResponse.EventInfo event1 = response.getBody().getEvents().get(0);
        assertEquals("evt-001", event1.getEventId());
        assertEquals(0, event1.getSequence());
        assertEquals("signal.accepted", event1.getEventType());
    }

    @Test
    void testQueryEventsWithPagination() {
        String signalId = "sig-001";
        Instant now = Instant.now();

        List<ExecutionEvent> mockEvents = new ArrayList<>();
        mockEvents.add(new ExecutionEvent(
                "evt-005", signalId, 5, ExecutionEventType.ORDER_FILLED, now, null, new HashMap<>(), now
        ));

        // Mock port responses
        when(executionEventPort.countBySignalId(signalId)).thenReturn(10L);
        when(executionEventPort.findBySignalIdAndSequenceRange(signalId, 5, 9))
                .thenReturn(mockEvents);

        // Execute with fromSequence=5, limit=5
        ResponseEntity<EventHistoryResponse> response = controller.queryEvents(signalId, 5, 5);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(5, response.getBody().getFromSequence());
        assertEquals(9, response.getBody().getToSequence());
        assertFalse(response.getBody().isHasMore()); // 10 total, last range is 5-9
    }

    @Test
    void testQueryEventsNotFound() {
        String signalId = "sig-unknown";

        // Mock port response - no events
        when(executionEventPort.countBySignalId(signalId)).thenReturn(0L);

        // Execute
        ResponseEntity<EventHistoryResponse> response = controller.queryEvents(signalId, 0, 50);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(0, response.getBody().getEvents().size());
        assertEquals(0, response.getBody().getTotalCount());
        assertFalse(response.getBody().isHasMore());
    }

    @Test
    void testQueryEventsInvalidSignalId() {
        // Execute with empty signal ID
        ResponseEntity<EventHistoryResponse> response = controller.queryEvents("", 0, 50);

        // Verify
        assertEquals(400, response.getStatusCodeValue());
        verify(executionEventPort, never()).countBySignalId(any());
    }

    @Test
    void testQueryEventsDefaultLimit() {
        String signalId = "sig-001";

        when(executionEventPort.countBySignalId(signalId)).thenReturn(100L);
        when(executionEventPort.findBySignalIdAndSequenceRange(signalId, 0, 49))
                .thenReturn(new ArrayList<>());

        // Execute with default limit
        ResponseEntity<EventHistoryResponse> response = controller.queryEvents(signalId, 0, 0);

        // Verify default limit of 50 is used
        verify(executionEventPort).findBySignalIdAndSequenceRange(signalId, 0, 49);
    }

    @Test
    void testQueryEventsMaxLimit() {
        String signalId = "sig-001";

        when(executionEventPort.countBySignalId(signalId)).thenReturn(1000L);
        when(executionEventPort.findBySignalIdAndSequenceRange(signalId, 0, 499))
                .thenReturn(new ArrayList<>());

        // Execute with limit > MAX_LIMIT
        ResponseEntity<EventHistoryResponse> response = controller.queryEvents(signalId, 0, 1000);

        // Verify MAX_LIMIT of 500 is used
        verify(executionEventPort).findBySignalIdAndSequenceRange(signalId, 0, 499);
    }
}

package io.marcus.api.websocket.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.application.executor.SyncExecutionEventInput;
import io.marcus.application.executor.SyncExecutionEventOutput;
import io.marcus.application.executor.SyncExecutionEventUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for ExecutorEventEventHandler.
 */
class ExecutorEventEventHandlerTest {

    @Mock
    private SyncExecutionEventUseCase syncExecutionEventUseCase;

    @Mock
    private WebSocketSession session;

    private ExecutorEventEventHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();

                handler = new ExecutorEventEventHandler(syncExecutionEventUseCase, objectMapper);
    }

    @Test
    void testHandleExecutionEventSuccessful() throws IOException {
        Instant now = Instant.now();

        // Create frame
        Map<String, Object> payload = Map.of(
                "eventId", "evt-001",
                "signalId", "sig-001",
                "sequence", 0,
                "eventType", "signal.accepted",
                "sentAt", now.toString(),
                "payload", Map.of("riskLevel", "medium")
        );

        Map<String, Object> frame = Map.of(
                "type", "execution_event",
                "payload", payload
        );

        JsonNode frameNode = objectMapper.convertValue(frame, JsonNode.class);

        // Mock use case response
        when(syncExecutionEventUseCase.execute(any(SyncExecutionEventInput.class)))
                .thenReturn(SyncExecutionEventOutput.ok("evt-001", "sig-001", now));

        // Handle event
        handler.handleExecutionEvent(session, frameNode);

        // Verify use case was called
        verify(syncExecutionEventUseCase, times(1)).execute(any(SyncExecutionEventInput.class));
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void testHandleExecutionEventOutOfOrder() throws IOException {
        Instant now = Instant.now();

        // Create frame with out-of-order sequence
        Map<String, Object> payload = Map.of(
                "eventId", "evt-002",
                "signalId", "sig-001",
                "sequence", 5,
                "eventType", "order.placed",
                "sentAt", now.toString(),
                "payload", Map.of()
        );

        Map<String, Object> frame = Map.of(
                "type", "execution_event",
                "payload", payload
        );

        JsonNode frameNode = objectMapper.convertValue(frame, JsonNode.class);

        // Mock use case response with error
        when(syncExecutionEventUseCase.execute(any(SyncExecutionEventInput.class)))
                .thenReturn(SyncExecutionEventOutput.error(
                        "evt-002",
                        "sig-001",
                        "OUT_OF_ORDER",
                        "Expected sequence 1, received 5",
                        now
                ));

        // Handle event
        handler.handleExecutionEvent(session, frameNode);

        // Verify use case was called
        verify(syncExecutionEventUseCase, times(1)).execute(any(SyncExecutionEventInput.class));
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void testHandleExecutionEventMissingRequiredFields() throws IOException {
        // Create frame with missing eventId
        Map<String, Object> payload = Map.of(
                "signalId", "sig-001",
                "sequence", 0,
                "eventType", "signal.accepted",
                "sentAt", Instant.now().toString()
        );

        Map<String, Object> frame = Map.of(
                "type", "execution_event",
                "payload", payload
        );

        JsonNode frameNode = objectMapper.convertValue(frame, JsonNode.class);

        // Handle event
        handler.handleExecutionEvent(session, frameNode);

        // Verify use case was NOT called (validation failed)
        verify(syncExecutionEventUseCase, never()).execute(any(SyncExecutionEventInput.class));
    }

    @Test
    void testHandleExecutionEventInvalidTimestamp() throws IOException {
        // Create frame with invalid timestamp
        Map<String, Object> payload = Map.of(
                "eventId", "evt-001",
                "signalId", "sig-001",
                "sequence", 0,
                "eventType", "signal.accepted",
                "sentAt", "not-a-timestamp"
        );

        Map<String, Object> frame = Map.of(
                "type", "execution_event",
                "payload", payload
        );

        JsonNode frameNode = objectMapper.convertValue(frame, JsonNode.class);

        // Handle event
        handler.handleExecutionEvent(session, frameNode);

        // Verify use case was NOT called (validation failed)
        verify(syncExecutionEventUseCase, never()).execute(any(SyncExecutionEventInput.class));
    }

}

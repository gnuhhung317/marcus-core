package io.marcus.api.controller.executor;

import io.marcus.domain.executor.ExecutionState;
import io.marcus.domain.executor.ExecutionStatePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for ExecutorRecoveryController.
 */
class ExecutorRecoveryControllerTest {

    @Mock
    private ExecutionStatePort executionStatePort;

    private ExecutorRecoveryController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ExecutorRecoveryController(executionStatePort);
    }

    @Test
    void testRecoveryWithKnownSignals() {
        String signalId1 = "sig-001";
        String signalId2 = "sig-002";

        Instant now = Instant.now();

        // Create mock states
        ExecutionState state1 = new ExecutionState(
                signalId1,
                ExecutionState.SignalState.OPEN,
                ExecutionState.OrderState.FILLED,
                ExecutionState.PositionState.OPENED,
                5,
                now.minusSeconds(10),
                null
        );

        ExecutionState state2 = new ExecutionState(
                signalId2,
                ExecutionState.SignalState.CLOSED,
                ExecutionState.OrderState.FILLED,
                ExecutionState.PositionState.CLOSED,
                8,
                now.minusSeconds(5),
                now
        );

        // Mock port responses
        when(executionStatePort.getState(signalId1)).thenReturn(Optional.of(state1));
        when(executionStatePort.getState(signalId2)).thenReturn(Optional.of(state2));

        // Create request
        ExecutorRecoveryRequest request = new ExecutorRecoveryRequest(Arrays.asList(signalId1, signalId2));

        // Execute
        ResponseEntity<ExecutorRecoveryResponse> response = controller.recovery(request);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getSignals().size());

        ExecutorRecoveryResponse.SignalRecoveryInfo signal1 = response.getBody().getSignals().get(0);
        assertEquals(signalId1, signal1.getSignalId());
        assertEquals(5, signal1.getLastSequence());
        assertEquals("OPEN", signal1.getSignalState());
        assertEquals("FILLED", signal1.getOrderState());
        assertEquals("OPENED", signal1.getPositionState());

        ExecutorRecoveryResponse.SignalRecoveryInfo signal2 = response.getBody().getSignals().get(1);
        assertEquals(signalId2, signal2.getSignalId());
        assertEquals(8, signal2.getLastSequence());
        assertEquals("CLOSED", signal2.getSignalState());
        assertEquals("FILLED", signal2.getOrderState());
        assertEquals("CLOSED", signal2.getPositionState());
    }

    @Test
    void testRecoveryWithUnknownSignals() {
        String signalId = "sig-unknown";

        // Mock port response - not found
        when(executionStatePort.getState(signalId)).thenReturn(Optional.empty());

        // Create request
        ExecutorRecoveryRequest request = new ExecutorRecoveryRequest(Arrays.asList(signalId));

        // Execute
        ResponseEntity<ExecutorRecoveryResponse> response = controller.recovery(request);

        // Verify
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getSignals().size());

        ExecutorRecoveryResponse.SignalRecoveryInfo signal = response.getBody().getSignals().get(0);
        assertEquals(signalId, signal.getSignalId());
        assertEquals(0, signal.getLastSequence());
        assertEquals("UNKNOWN", signal.getSignalState());
    }

    @Test
    void testRecoveryWithEmptySignalIds() {
        // Create request with empty list
        ExecutorRecoveryRequest request = new ExecutorRecoveryRequest(Arrays.asList());

        // Execute
        ResponseEntity<ExecutorRecoveryResponse> response = controller.recovery(request);

        // Verify
        assertEquals(400, response.getStatusCodeValue());
        verify(executionStatePort, never()).getState(any());
    }

    @Test
    void testRecoveryWithNullSignalIds() {
        // Create request with null list
        ExecutorRecoveryRequest request = new ExecutorRecoveryRequest(null);

        // Execute
        ResponseEntity<ExecutorRecoveryResponse> response = controller.recovery(request);

        // Verify
        assertEquals(400, response.getStatusCodeValue());
        verify(executionStatePort, never()).getState(any());
    }
}

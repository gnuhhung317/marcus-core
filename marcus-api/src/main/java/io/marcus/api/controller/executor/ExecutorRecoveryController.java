package io.marcus.api.controller.executor;

import io.marcus.domain.executor.ExecutionState;
import io.marcus.domain.executor.ExecutionStatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for executor recovery operations.
 */
@RestController
@RequestMapping("/api/executor")
@Slf4j
@RequiredArgsConstructor
public class ExecutorRecoveryController {

    private final ExecutionStatePort executionStatePort;

    /**
     * Handle executor recovery request.
     * Returns the last known state for each signal ID.
     */
    @PostMapping("/recovery")
    public ResponseEntity<ExecutorRecoveryResponse> recovery(
            @RequestBody ExecutorRecoveryRequest request
    ) {
        log.info("Recovery request for {} signals", request.getSignalIds().size());

        if (request.getSignalIds() == null || request.getSignalIds().isEmpty()) {
            log.warn("Recovery request with empty signal IDs");
            return ResponseEntity.badRequest().build();
        }

        // Fetch recovery information for each signal
        List<ExecutorRecoveryResponse.SignalRecoveryInfo> signals = request.getSignalIds()
                .stream()
                .map(signalId -> {
                    Optional<?> stateOpt = executionStatePort.getState(signalId);
                    
                    if (stateOpt.isEmpty()) {
                        log.debug("No state found for signal: {}", signalId);
                        // Return a default recovery info for unknown signals
                        return ExecutorRecoveryResponse.SignalRecoveryInfo.builder()
                                .signalId(signalId)
                                .lastSequence(0)
                                .signalState("UNKNOWN")
                                .orderState("NONE")
                                .positionState("NONE")
                                .build();
                    }

                    // Extract state information from ExecutionState
                    Object stateObj = stateOpt.get();
                    if (stateObj instanceof ExecutionState) {
                        ExecutionState state = (ExecutionState) stateObj;
                        return ExecutorRecoveryResponse.SignalRecoveryInfo.builder()
                                .signalId(signalId)
                                .lastSequence(state.getLastSequence())
                                .signalState(state.getSignalState().name())
                                .orderState(state.getOrderState().name())
                                .positionState(state.getPositionState().name())
                                .lastEventTime(state.getLastEventTime())
                                .closedAt(state.getClosedAt())
                                .build();
                    } else {
                        log.warn("Unexpected state type for signal: {}", signalId);
                        return ExecutorRecoveryResponse.SignalRecoveryInfo.builder()
                                .signalId(signalId)
                                .lastSequence(0)
                                .signalState("UNKNOWN")
                                .orderState("NONE")
                                .positionState("NONE")
                                .build();
                    }
                })
                .collect(Collectors.toList());

        ExecutorRecoveryResponse response = ExecutorRecoveryResponse.builder()
                .signals(signals)
                .recoveredAt(Instant.now())
                .build();

        log.info("Recovery response prepared for {} signals", signals.size());
        return ResponseEntity.ok(response);
    }
}


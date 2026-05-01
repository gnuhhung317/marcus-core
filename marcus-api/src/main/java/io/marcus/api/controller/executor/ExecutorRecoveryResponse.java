package io.marcus.api.controller.executor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for executor recovery endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutorRecoveryResponse {
    
    /**
     * List of signal recovery information.
     */
    private List<SignalRecoveryInfo> signals;
    
    /**
     * Recovery response timestamp.
     */
    private Instant recoveredAt;
    
    /**
     * Signal recovery information.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignalRecoveryInfo {
        
        /**
         * Signal ID.
         */
        private String signalId;
        
        /**
         * Last sequence number processed.
         */
        private int lastSequence;
        
        /**
         * Current signal state: ACCEPTED, REJECTED, OPEN, CLOSED.
         */
        private String signalState;
        
        /**
         * Current order state: NONE, PLACED, FILLED, FAILED, CANCELED.
         */
        private String orderState;
        
        /**
         * Current position state: NONE, OPENED, UPDATING, CLOSED.
         */
        private String positionState;
        
        /**
         * Last event timestamp.
         */
        private Instant lastEventTime;
        
        /**
         * Closed at timestamp (if position is closed).
         */
        private Instant closedAt;
    }
}

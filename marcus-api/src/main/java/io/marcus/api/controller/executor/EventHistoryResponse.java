package io.marcus.api.controller.executor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for event history query endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventHistoryResponse {
    
    /**
     * Signal ID.
     */
    private String signalId;
    
    /**
     * List of events for the signal.
     */
    private List<EventInfo> events;
    
    /**
     * Total count of events.
     */
    private long totalCount;
    
    /**
     * Current query sequence range (from).
     */
    private int fromSequence;
    
    /**
     * Current query sequence range (to).
     */
    private int toSequence;
    
    /**
     * Whether there are more events after the current range.
     */
    private boolean hasMore;
    
    /**
     * Query timestamp.
     */
    private Instant queriedAt;
    
    /**
     * Individual event information.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventInfo {
        
        /**
         * Event ID (unique identifier).
         */
        private String eventId;
        
        /**
         * Signal ID.
         */
        private String signalId;
        
        /**
         * Sequence number.
         */
        private int sequence;
        
        /**
         * Event type (e.g., signal.accepted, order.placed, order.filled).
         */
        private String eventType;
        
        /**
         * Time event was sent from executor.
         */
        private Instant sentAt;
        
        /**
         * Exchange/backend received time.
         */
        private Instant exchangeTime;
        
        /**
         * Event payload (JSON).
         */
        private Object payload;
        
        /**
         * Event creation timestamp.
         */
        private Instant createdAt;
    }
}

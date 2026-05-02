package io.marcus.api.controller.executor;

import io.marcus.domain.executor.ExecutionEvent;
import io.marcus.domain.executor.ExecutionEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for event history query operations.
 */
@RestController
@RequestMapping("/api/executor")
@Slf4j
@RequiredArgsConstructor
public class EventHistoryController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;

    private final ExecutionEventPort executionEventPort;

    /**
     * Query event history for a signal.
     * 
     * @param signalId the signal ID to query
     * @param fromSequence starting sequence number (inclusive, default 0)
     * @param limit max number of events to return (default 50, max 500)
     * @return event history with pagination info
     */
    @GetMapping("/events")
    public ResponseEntity<EventHistoryResponse> queryEvents(
            @RequestParam String signalId,
            @RequestParam(defaultValue = "0") int fromSequence,
            @RequestParam(defaultValue = "50") int limit
    ) {
        log.info("Event history query for signalId={}, fromSequence={}, limit={}", 
                signalId, fromSequence, limit);

        // Validate parameters
        if (signalId == null || signalId.trim().isEmpty()) {
            log.warn("Event history query with empty signalId");
            return ResponseEntity.badRequest().build();
        }

        if (fromSequence < 0) {
            fromSequence = 0;
        }

        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        } else if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }

        try {
            // Get total count of events for the signal
            long totalCount = executionEventPort.countBySignalId(signalId);

            if (totalCount == 0) {
                log.debug("No events found for signal: {}", signalId);
                return ResponseEntity.ok(
                        EventHistoryResponse.builder()
                                .signalId(signalId)
                                .events(List.of())
                                .totalCount(0)
                                .fromSequence(fromSequence)
                                .toSequence(fromSequence + limit - 1)
                                .hasMore(false)
                                .queriedAt(Instant.now())
                                .build()
                );
            }

            // Query events in the range
            int toSequence = fromSequence + limit - 1;
            List<ExecutionEvent> events = executionEventPort.findBySignalIdAndSequenceRange(
                    signalId,
                    fromSequence,
                    toSequence
            );

            log.debug("Retrieved {} events for signal: {}, range [{}, {}]", 
                    events.size(), signalId, fromSequence, toSequence);

            // Map to response DTOs
            List<EventHistoryResponse.EventInfo> eventInfos = events.stream()
                    .map(event -> EventHistoryResponse.EventInfo.builder()
                            .eventId(event.getEventId())
                            .signalId(event.getSignalId())
                            .sequence(event.getSequence())
                            .eventType(event.getEventType().getEventTypeCode())
                            .sentAt(event.getSentAt())
                            .exchangeTime(event.getExchangeTime())
                            .payload(event.getPayload())
                            .createdAt(event.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

            // Check if there are more events
            boolean hasMore = (toSequence + 1) < totalCount;

            EventHistoryResponse response = EventHistoryResponse.builder()
                    .signalId(signalId)
                    .events(eventInfos)
                    .totalCount(totalCount)
                    .fromSequence(fromSequence)
                    .toSequence(toSequence)
                    .hasMore(hasMore)
                    .queriedAt(Instant.now())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error querying event history for signal: {}", signalId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

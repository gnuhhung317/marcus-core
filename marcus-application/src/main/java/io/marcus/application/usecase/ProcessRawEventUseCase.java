package io.marcus.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.RawEvent;
import io.marcus.domain.model.Signal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Processes persisted RawEvent objects and routes them to domain use cases.
 * Currently supports `ingest` messages which are mapped to `Signal` and
 * forwarded to `CaptureSignalUseCase`.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessRawEventUseCase {

    private final CaptureSignalUseCase captureSignalUseCase;
    private final ObjectMapper objectMapper;

    public void execute(RawEvent rawEvent) {
        if (rawEvent == null) return;

        String type = rawEvent.getType();
        if ("ingest".equalsIgnoreCase(type)) {
            try {
                // Map payload to Signal domain model
                Signal signal = objectMapper.convertValue(rawEvent.getPayload(), Signal.class);
                // Ensure botId from envelope is preserved
                if (signal.getBotId() == null || signal.getBotId().isBlank()) {
                    signal.setBotId(rawEvent.getBotId());
                }

                // Ensure signalId exists: use eventId as fallback
                if (signal.getSignalId() == null || signal.getSignalId().isBlank()) {
                    signal.setSignalId(rawEvent.getEventId());
                }

                captureSignalUseCase.execute(signal);
            } catch (Exception e) {
                log.error("Failed to process ingest rawEvent {}: {}", rawEvent.getEventId(), e.getMessage(), e);
            }
        } else {
            log.debug("Unsupported raw event type for processing: {}", type);
        }
    }
}

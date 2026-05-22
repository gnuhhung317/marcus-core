package io.marcus.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.application.dto.CaptureSignalRequest;
import io.marcus.domain.model.RawEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Processes persisted RawEvent objects and routes them to domain use cases.
 * Forwarding to `CaptureSignalUseCase`.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessRawEventUseCase {

    private final CaptureSignalUseCase captureSignalUseCase;
    private final ObjectMapper objectMapper;

    public void execute(RawEvent rawEvent) {
        if (rawEvent == null) {
            return;
        }

        String type = rawEvent.getType();
        if ("ingest".equalsIgnoreCase(type)) {
            try {
                // Map payload to CaptureSignalRequest DTO
                CaptureSignalRequest request = objectMapper.convertValue(rawEvent.getPayload(), CaptureSignalRequest.class);

                // Ensure botId and signalId are populated
                String botId = (request.botId() == null || request.botId().isBlank()) ? rawEvent.getBotId() : request.botId();
                String signalId = (request.signalId() == null || request.signalId().isBlank()) ? rawEvent.getEventId() : request.signalId();

                if (!botId.equals(request.botId()) || !signalId.equals(request.signalId())) {
                    request = new CaptureSignalRequest(
                            signalId,
                            botId,
                            request.symbol(),
                            request.action(),
                            request.entry(),
                            request.stopLoss(),
                            request.takeProfit(),
                            request.status(),
                            request.generatedTimestamp(),
                            request.timeframe(),
                            request.metadata()
                    );
                }

                captureSignalUseCase.execute(request);
            } catch (Exception e) {
                log.error("Failed to process ingest rawEvent {}: {}", rawEvent.getEventId(), e.getMessage(), e);
            }
        } else {
            log.debug("Unsupported raw event type for processing: {}", type);
        }
    }
}

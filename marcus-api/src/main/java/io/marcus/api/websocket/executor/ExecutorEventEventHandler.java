package io.marcus.api.websocket.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.websocket.ExecutorHandshakeInterceptor;
import io.marcus.application.executor.SyncExecutionEventInput;
import io.marcus.application.executor.SyncExecutionEventOutput;
import io.marcus.application.executor.SyncExecutionEventUseCase;
import io.marcus.domain.model.RawEvent;
import io.marcus.domain.port.RawEventPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for execution_event frames from executor. Processes events through
 * SyncExecutionEventUseCase and sends back ACK.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExecutorEventEventHandler {

    private final SyncExecutionEventUseCase syncExecutionEventUseCase;
    private final RawEventPersistencePort rawEventPersistencePort;
    private final ObjectMapper objectMapper;

    /**
     * Handle execution_event frame. Frame format: {type: "execution_event",
     * payload: {...event data...}}
     */
    public void handleExecutionEvent(WebSocketSession session, JsonNode frameRoot) throws IOException {
        try {
            JsonNode payload = frameRoot.path("payload");

            // Extract event fields
            String eventId = payload.path("eventId").asText("");
            String signalId = payload.path("signalId").asText("");
            int sequence = payload.path("sequence").asInt(-1);
            String eventType = payload.path("eventType").asText("");
            String sentAtStr = payload.path("sentAt").asText("");
            String exchangeTimeStr = payload.path("exchangeTime").asText("");
            JsonNode eventPayload = payload.path("payload");

            // Validate required fields
            if (eventId.isBlank() || signalId.isBlank() || sequence < 0 || eventType.isBlank() || sentAtStr.isBlank()) {
                log.warn("Invalid execution_event frame: missing required fields");
                sendAck(session, eventId, "ERROR", "INVALID_STATE", "Missing required fields");
                return;
            }

            // Parse timestamps
            Instant sentAt;
            Instant exchangeTime = null;
            try {
                sentAt = Instant.parse(sentAtStr);
                if (!exchangeTimeStr.isBlank()) {
                    exchangeTime = Instant.parse(exchangeTimeStr);
                }
            } catch (Exception e) {
                log.warn("Invalid timestamp format in execution_event", e);
                sendAck(session, eventId, "ERROR", "INVALID_STATE", "Invalid timestamp format");
                return;
            }

            // Create use case input
            Object payloadObj = eventPayload.isNull() ? Map.of() : eventPayload;
            SyncExecutionEventInput input = new SyncExecutionEventInput(
                    eventId,
                    signalId,
                    sequence,
                    eventType,
                    sentAt,
                    exchangeTime,
                    payloadObj
            );

            // Execute use case
            SyncExecutionEventOutput output = syncExecutionEventUseCase.execute(input);

            // Send ACK
            if (output.isSuccess()) {
                persistExecutorActivity(session, eventId, signalId, eventType, eventPayload);
                log.info("Execution event accepted: eventId={}, signalId={}, sequence={}", eventId, signalId, sequence);
                sendAck(session, eventId, "OK", null, null);
            } else {
                log.warn("Execution event rejected: eventId={}, errorCode={}, message={}",
                        eventId, output.getErrorCode(), output.getErrorMessage());
                sendAck(session, eventId, "ERROR", output.getErrorCode(), output.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Error handling execution_event", e);
            String eventId = frameRoot.path("payload").path("eventId").asText("unknown");
            sendAck(session, eventId, "ERROR", "INTERNAL_ERROR", "Internal error: " + e.getMessage());
        }
    }

    private void persistExecutorActivity(
            WebSocketSession session,
            String eventId,
            String signalId,
            String eventType,
            JsonNode eventPayload
    ) {
        String botId = (String) session.getAttributes().get("botId");
        String subscriptionId = (String) session.getAttributes()
                .get(ExecutorHandshakeInterceptor.USER_SUBSCRIPTION_ID_ATTRIBUTE);
        if (botId == null || botId.isBlank() || subscriptionId == null || subscriptionId.isBlank()) {
            log.warn("Execution event activity was not persisted because session ownership is missing. signalId={}", signalId);
            return;
        }

        Map<String, Object> payload = eventPayload.isObject()
                ? objectMapper.convertValue(eventPayload, Map.class)
                : new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("signalId", signalId);
        rawEventPersistencePort.save(RawEvent.builder()
                .eventId("execution-log-" + eventId)
                .botId(botId)
                .userSubscriptionId(subscriptionId)
                .idempotencyKey("execution-log-" + eventId)
                .correlationId(signalId)
                .type("execution_event")
                .payload(payload)
                .receivedAt(Instant.now())
                .sourceConnId(session.getId())
                .processed(true)
                .processedAt(Instant.now())
                .build());
    }

    /**
     * Send execution_ack frame.
     */
    private void sendAck(WebSocketSession session, String eventId, String status, String errorCode, String errorMessage) throws IOException {
        Map<String, Object> ackPayload = new HashMap<>();
        ackPayload.put("eventId", eventId);
        ackPayload.put("status", status);
        ackPayload.put("receivedAt", Instant.now().toString());

        if (errorCode != null) {
            ackPayload.put("errorCode", errorCode);
        }
        if (errorMessage != null) {
            ackPayload.put("errorMessage", errorMessage);
        }

        Map<String, Object> ackFrame = new HashMap<>();
        ackFrame.put("type", "execution_ack");
        ackFrame.put("payload", ackPayload);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ackFrame)));
    }
}

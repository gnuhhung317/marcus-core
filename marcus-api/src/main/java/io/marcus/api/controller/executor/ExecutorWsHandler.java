package io.marcus.api.controller.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.application.usecase.CaptureSignalUseCase;
import io.marcus.domain.model.RawEvent;
import io.marcus.domain.port.RawEventPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for authenticated executor connections.
 * Validates HMAC signatures, persists raw events, and routes to use cases.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExecutorWsHandler extends TextWebSocketHandler {

    private final RawEventPersistencePort rawEventPersistencePort;
    private final CaptureSignalUseCase captureSignalUseCase;
    private final io.marcus.application.usecase.ProcessRawEventUseCase processRawEventUseCase;
    private final ObjectMapper objectMapper;

    // Per-connection metadata: sessionId -> {botId, connId, authenticated}
    private final Map<String, ExecutorConnection> connections = new ConcurrentHashMap<>();

    private static final long TIMESTAMP_WINDOW_SECONDS = 300; // 5 minutes
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String connId = session.getId();
        log.info("Executor WebSocket connection established: {}", connId);
        connections.put(connId, new ExecutorConnection(connId, null, false));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String connId = session.getId();
        ExecutorConnection conn = connections.get(connId);

        if (conn == null) {
            log.warn("Received message from unknown connection: {}", connId);
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }

        try {
            JsonNode envelope = objectMapper.readTree(message.getPayload());
            String messageType = envelope.get("type").asText();

            if ("handshake".equals(messageType)) {
                handleHandshake(session, conn, envelope);
            } else if (conn.isAuthenticated()) {
                handleFrameAfterAuth(session, conn, envelope);
            } else {
                log.warn("Received non-handshake message before authentication: {}", connId);
                session.close(CloseStatus.POLICY_VIOLATION);
            }
        } catch (Exception e) {
            log.error("Error processing message from {}: {}", connId, e.getMessage(), e);
            sendError(session, "INVALID_MESSAGE", e.getMessage());
        }
    }

    private void handleHandshake(WebSocketSession session, ExecutorConnection conn, JsonNode envelope) throws Exception {
        String connId = session.getId();
        String botId = envelope.get("botId").asText();
        String timestamp = envelope.get("timestamp").asText();
        String signature = envelope.get("signature").asText();
        JsonNode payload = envelope.get("payload");

        // Validate timestamp (within 5 minutes)
        Instant messageTime = Instant.parse(timestamp);
        Instant now = Instant.now();
        if (Math.abs(now.getEpochSecond() - messageTime.getEpochSecond()) > TIMESTAMP_WINDOW_SECONDS) {
            log.warn("Handshake timestamp expired for botId {}: {}", botId, timestamp);
            sendError(session, "EXPIRED_TIMESTAMP", "Timestamp is older than 5 minutes");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // Validate signature: HMAC-SHA256(botId|timestamp|base64(payload), botSecret)
        // NOTE: botSecret lookup is a placeholder; implement actual secret retrieval from bot registry
        String botSecret = lookupBotSecret(botId);
        if (botSecret == null) {
            log.warn("No secret found for botId: {}", botId);
            sendError(session, "UNKNOWN_BOT", "Bot not found");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String payloadBase64 = Base64.getEncoder().encodeToString(
            objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8)
        );
        String signatureInput = botId + "|" + timestamp + "|" + payloadBase64;
        String computedSignature = computeHmacSha256(signatureInput, botSecret);

        if (!computedSignature.equals(signature)) {
            log.warn("Invalid signature for botId {}", botId);
            sendError(session, "INVALID_SIGNATURE", "Signature validation failed");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // Authentication successful
        conn.setBotId(botId);
        conn.setAuthenticated(true);
        log.info("Executor authenticated: botId={}, connId={}", botId, connId);

        // Send handshake-ack
        sendFrame(session, "handshake-ack", Map.of(
            "connId", connId,
            "botId", botId,
            "timestamp", Instant.now().toString(),
            "status", "OK"
        ));
    }

    private void handleFrameAfterAuth(WebSocketSession session, ExecutorConnection conn, JsonNode envelope) throws Exception {
        String connId = session.getId();
        String botId = conn.getBotId();
        String messageType = envelope.get("type").asText();

        String eventId = envelope.get("eventId").asText();
        String idempotencyKey = envelope.get("idempotencyKey").asText();
        String correlationId = envelope.get("correlationId").asText();
        JsonNode payload = envelope.get("payload");

        // Validate required fields
        if (eventId == null || idempotencyKey == null || correlationId == null) {
            log.warn("Missing required fields in message: botId={}, connId={}", botId, connId);
            sendError(session, "MISSING_FIELDS", "eventId, idempotencyKey, and correlationId are required");
            return;
        }

        // Persist raw event
        RawEvent rawEvent = RawEvent.builder()
            .eventId(eventId)
            .botId(botId)
            .idempotencyKey(idempotencyKey)
            .correlationId(correlationId)
            .type(messageType)
            .payload(objectMapper.convertValue(payload, Map.class))
            .receivedAt(Instant.now())
            .sourceConnId(connId)
            .processed(false)
            .build();

        RawEvent persisted = rawEventPersistencePort.save(rawEvent);

        // Send ack immediately
        sendFrame(session, "ack", Map.of(
            "eventId", eventId,
            "idempotencyKey", idempotencyKey,
            "status", "PERSISTED",
            "sequenceNo", persisted.getSequenceNo()
        ));

        // Route to use case based on message type
        if ("ingest".equals(messageType)) {
            // Forward persisted raw event to application layer for processing
            try {
                processRawEventUseCase.execute(persisted);
            } catch (Exception e) {
                log.error("Error processing raw event in application layer: {}", e.getMessage(), e);
            }
            log.debug("Raw event persisted and dispatched: eventId={}, botId={}, seq={}", eventId, botId, persisted.getSequenceNo());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String connId = session.getId();
        connections.remove(connId);
        log.info("Executor WebSocket connection closed: {} ({})", connId, status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String connId = session.getId();
        log.error("WebSocket transport error for {}: {}", connId, exception.getMessage(), exception);
    }

    private void sendFrame(WebSocketSession session, String type, Map<String, Object> data) throws IOException {
        Map<String, Object> frame = new HashMap<>(data);
        frame.put("type", type);
        frame.put("timestamp", Instant.now().toString());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
    }

    private void sendError(WebSocketSession session, String errorCode, String message) throws IOException {
        sendFrame(session, "error", Map.of(
            "code", errorCode,
            "message", message
        ));
    }

    private String computeHmacSha256(String input, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                0,
                secret.getBytes(StandardCharsets.UTF_8).length,
                HMAC_ALGORITHM
            );
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }

    private String lookupBotSecret(String botId) {
        // TODO: Implement actual bot secret lookup from bot registry/database
        // For now, placeholder that returns null (forces auth failure)
        return null;
    }

    /**
     * Per-connection state holder.
     */
    private static class ExecutorConnection {
        private final String connId;
        private String botId;
        private boolean authenticated;

        ExecutorConnection(String connId, String botId, boolean authenticated) {
            this.connId = connId;
            this.botId = botId;
            this.authenticated = authenticated;
        }

        String getConnId() { return connId; }
        String getBotId() { return botId; }
        void setBotId(String botId) { this.botId = botId; }
        boolean isAuthenticated() { return authenticated; }
        void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
    }
}

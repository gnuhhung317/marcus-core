package io.marcus.api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.websocket.executor.AuditPushEventHandler;
import io.marcus.api.websocket.executor.ExecutorEventEventHandler;
import io.marcus.domain.port.ExecutorOnlineStatusPort;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.repository.SignalRepository;
import io.marcus.domain.vo.SignalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExecutorWebSocketHandler extends TextWebSocketHandler {

    private static final long HANDSHAKE_MAX_AGE_SECONDS = 300L;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final ExecutorSessionRegistry sessionRegistry;
    private final UserSubscriptionPersistencePort userSubscriptionPersistencePort;
    private final SignalRepository signalRepository;
    private final ExecutorOnlineStatusPort executorOnlineStatusPort;
    @Lazy
    private final ExecutorEventEventHandler executorEventEventHandler;
    @Lazy
    private final AuditPushEventHandler auditPushEventHandler;

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
        String wsToken = (String) session.getAttributes().get(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE);
        if (wsToken != null) {
            executorOnlineStatusPort.markOffline(wsToken);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String frameType = root.path("type").asText("").trim().toLowerCase();

            if ("handshake".equals(frameType)) {
                handleHandshake(session, root);
                return;
            }

            if ("heartbeat".equals(frameType)) {
                String wsToken = (String) session.getAttributes().get(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE);
                if (wsToken != null) {
                    executorOnlineStatusPort.markOnline(wsToken, 30);
                }
                sendFrame(session, buildAckFrame("heartbeat", "ok", null));
                return;
            }

            if ("subscribe".equals(frameType)) {
                handleSubscribe(session, root);
                return;
            }

            if ("execution_event".equals(frameType)) {
                executorEventEventHandler.handleExecutionEvent(session, root);
                return;
            }

            if ("audit-push".equals(frameType)) {
                auditPushEventHandler.handleAuditPush(session, root);
                return;
            }

            if ("signal_ack".equals(frameType)) {
                handleSignalAck(session, root);
                return;
            }

            sendFrame(session, buildErrorFrame("unsupported_frame", "Unsupported frame type: " + frameType));
            session.close(CloseStatus.PROTOCOL_ERROR);
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            try {
                sendFrame(session, buildErrorFrame("internal_error", "Error processing message: " + e.getMessage()));
            } catch (IOException ioe) {
                log.error("Error sending error frame", ioe);
            }
        }
    }

    private void handleHandshake(WebSocketSession session, JsonNode root) throws IOException {
        JsonNode payload = root.path("payload");
        String botId = root.path("botId").asText(root.path("bot_id").asText(""));
        String timestamp = root.path("timestamp").asText("");
        String signature = root.path("signature").asText("");
        String wsToken = (String) session.getAttributes().get(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE);

        if (botId.isBlank() || timestamp.isBlank() || signature.isBlank() || wsToken == null || wsToken.isBlank()) {
            sendFrame(session, buildErrorFrame("invalid_handshake", "botId, timestamp, signature and ws_token are required"));
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        if (!isHandshakeTimestampFresh(timestamp)) {
            sendFrame(session, buildErrorFrame("expired_handshake", "Handshake timestamp is too old"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String expectedSignature = signHandshake(botId, timestamp, payload, wsToken);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        )) {
            sendFrame(session, buildErrorFrame("invalid_signature", "Handshake signature validation failed"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        var subscription = userSubscriptionPersistencePort.findActiveByBotIdAndWsToken(botId, wsToken);
        if (subscription.isEmpty()) {
            sendFrame(session, buildErrorFrame("unauthorized", "No active subscription matches the websocket token"));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        sessionRegistry.register(
                wsToken,
                botId,
                subscription.get().getUserSubscriptionId(),
                session
        );
        session.getAttributes().put(ExecutorHandshakeInterceptor.USER_ID_ATTRIBUTE, subscription.get().getUserId());
        session.getAttributes().put("botId", botId);
        userSubscriptionPersistencePort.markExecutorConnected(subscription.get().getUserSubscriptionId(), true);
        executorOnlineStatusPort.markOnline(wsToken, 30);
        sendFrame(session, buildAckFrame("handshake", "ok", botId));
    }

    private void handleSubscribe(WebSocketSession session, JsonNode root) throws IOException {
        JsonNode payload = root.path("payload");
        String botId = payload.path("botId").asText(payload.path("bot_id").asText(""));
        String wsToken = (String) session.getAttributes().get(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE);

        if (botId.isBlank() || wsToken == null || wsToken.isBlank()) {
            sendFrame(session, buildErrorFrame("invalid_subscribe", "botId and ws_token are required"));
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        var subscription = userSubscriptionPersistencePort.findActiveByBotIdAndWsToken(botId, wsToken);
        if (subscription.isEmpty()) {
            sendFrame(session, buildErrorFrame("unauthorized", "No active subscription matches the websocket token"));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        sessionRegistry.register(
                wsToken,
                botId,
                subscription.get().getUserSubscriptionId(),
                session
        );
        session.getAttributes().put(ExecutorHandshakeInterceptor.USER_ID_ATTRIBUTE, subscription.get().getUserId());
        session.getAttributes().put("botId", botId);
        userSubscriptionPersistencePort.markExecutorConnected(subscription.get().getUserSubscriptionId(), true);
        executorOnlineStatusPort.markOnline(wsToken, 30);
        sendFrame(session, buildAckFrame("subscribe", "ok", botId));
    }

    private Map<String, Object> buildAckFrame(String ackType, String status, String botId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        payload.put("ack_type", ackType);
        if (botId != null) {
            payload.put("bot_id", botId);
        }

        Map<String, Object> frame = new HashMap<>();
        frame.put("type", "ack");
        frame.put("payload", payload);
        return frame;
    }

    private Map<String, Object> buildErrorFrame(String code, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("message", message);

        Map<String, Object> frame = new HashMap<>();
        frame.put("type", "system");
        frame.put("payload", payload);
        return frame;
    }

    public void sendFrame(WebSocketSession session, Map<String, Object> frame) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
    }

    private boolean isHandshakeTimestampFresh(String timestamp) {
        try {
            Instant sentAt = Instant.parse(timestamp);
            long ageSeconds = Math.abs(Instant.now().getEpochSecond() - sentAt.getEpochSecond());
            return ageSeconds <= HANDSHAKE_MAX_AGE_SECONDS;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private String signHandshake(String botId, String timestamp, JsonNode payload, String wsToken) throws IOException {
        String payloadJson = objectMapper.writeValueAsString(payload);
        String payloadBase64 = Base64.getEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String message = botId + "|" + timestamp + "|" + payloadBase64;

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(wsToken.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception ex) {
            throw new IOException("Failed to sign handshake", ex);
        }
    }

    private void handleSignalAck(WebSocketSession session, JsonNode root) {
        JsonNode payload = root.path("payload");
        String signalId = payload.path("signal_id").asText(payload.path("signalId").asText(""));
        String botId = (String) session.getAttributes().get("botId");

        if (signalId.isBlank()) {
            log.warn("[WebSocket] Received signal_ack with empty signalId from botId={}", botId);
            return;
        }

        log.info("[WebSocket] Received delivery ACK for signalId={} from botId={}", signalId, botId);
        try {
            signalRepository.updateStatus(signalId, SignalStatus.ACKNOWLEDGED);
        } catch (Exception e) {
            log.error("[WebSocket] Failed to update signal status for signalId={}: {}", signalId, e.getMessage());
        }
    }
}

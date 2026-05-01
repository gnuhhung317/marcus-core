package io.marcus.api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.websocket.executor.ExecutorEventEventHandler;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExecutorWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ExecutorSessionRegistry sessionRegistry;
    private final UserSubscriptionPersistencePort userSubscriptionPersistencePort;
    private final ExecutorEventEventHandler executorEventEventHandler;

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String frameType = root.path("type").asText("").trim().toLowerCase();

            if ("subscribe".equals(frameType)) {
                handleSubscribe(session, root);
                return;
            }

            if ("heartbeat".equals(frameType)) {
                sendFrame(session, buildAckFrame("heartbeat", "ok", null));
                return;
            }

            if ("execution_event".equals(frameType)) {
                executorEventEventHandler.handleExecutionEvent(session, root);
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

    private void handleSubscribe(WebSocketSession session, JsonNode root) throws IOException {
        JsonNode payload = root.path("payload");
        String botId = payload.path("bot_id").asText(payload.path("botId").asText(""));
        String wsToken = (String) session.getAttributes().get(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE);

        if (botId.isBlank() || wsToken == null || wsToken.isBlank()) {
            sendFrame(session, buildErrorFrame("invalid_subscribe", "bot_id and ws_token are required"));
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        var subscription = userSubscriptionPersistencePort.findActiveByBotIdAndWsToken(botId, wsToken);
        if (subscription.isEmpty()) {
            sendFrame(session, buildErrorFrame("unauthorized", "No active subscription matches the websocket token"));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        sessionRegistry.register(botId, session);
        userSubscriptionPersistencePort.markExecutorConnected(subscription.get().getUserSubscriptionId(), true);
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
}


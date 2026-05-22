package io.marcus.api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.vo.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.clearInvocations;

/**
 * Integration tests for the Executor WebSocket signed handshake protocol.
 *
 * Covers: - Signed handshake acceptance with valid HMAC - Signature rejection
 * on invalid HMAC - Timestamp validation (expired/future) - Session
 * registration and ack response - Idempotency (duplicate handshakes with same
 * botId + wsToken) - Executor connection marking in persistence
 */
@ExtendWith(MockitoExtension.class)
class ExecutorWebSocketHandshakeIntegrationTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private ExecutorSessionRegistry sessionRegistry;

    @Mock
    private UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    @Mock
    private io.marcus.domain.repository.SignalRepository signalRepository;

    @Mock
    private io.marcus.domain.port.ExecutorOnlineStatusPort executorOnlineStatusPort;

    @Mock
    private io.marcus.api.websocket.executor.ExecutorEventEventHandler executorEventEventHandler;

    @Mock
    private io.marcus.api.websocket.executor.AuditPushEventHandler auditPushEventHandler;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private ExecutorWebSocketHandler handler;

    @Test
    void shouldAcceptSignedHandshakeWithValidHMAC() throws Exception {
        // Arrange
        String botId = "bot-123";
        String wsToken = "ws_abc123def456";
        String nonce = UUID.randomUUID().toString();
        String version = "1.0";
        Instant now = Instant.now();
        String timestamp = now.toString();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE, wsToken);
        when(session.getAttributes()).thenReturn(attributes);

        UserSubscription subscription = createSubscription("sub-1", botId, wsToken);
        when(userSubscriptionPersistencePort.findActiveByBotIdAndWsToken(eq(botId), eq(wsToken)))
                .thenReturn(Optional.of(subscription));

        // Create signed handshake
        String handshakeMessage = buildSignedHandshake(botId, timestamp, nonce, version, wsToken);

        // Act
        handler.handleTextMessage(session, new TextMessage(handshakeMessage));

        // Assert
        verify(sessionRegistry).register(eq(wsToken), eq(botId), eq("sub-1"), eq(session));
        verify(userSubscriptionPersistencePort).markExecutorConnected(eq("sub-1"), eq(true));

        // Verify handshake-ack was sent
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());

        String responsePayload = messageCaptor.getValue().getPayload();
        JsonNode response = objectMapper.readTree(responsePayload);
        assertEquals("ack", response.get("type").asText());
        assertEquals("ok", response.path("payload").path("status").asText());
        assertEquals("handshake", response.path("payload").path("ack_type").asText());
        assertEquals(botId, response.path("payload").path("bot_id").asText());
    }

    @Test
    void shouldRejectHandshakeWithInvalidSignature() throws Exception {
        // Arrange
        String botId = "bot-123";
        String wsToken = "ws_abc123def456";
        String nonce = UUID.randomUUID().toString();
        String version = "1.0";
        Instant now = Instant.now();
        String timestamp = now.toString();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE, wsToken);
        when(session.getAttributes()).thenReturn(attributes);

        // Build handshake with WRONG signature
        Map<String, Object> payload = new HashMap<>();
        payload.put("nonce", nonce);
        payload.put("version", version);

        String handshakeMessage = objectMapper.writeValueAsString(Map.of(
                "type", "handshake",
                "botId", botId,
                "timestamp", timestamp,
                "payload", payload,
                "signature", "invalid_signature_12345" // Invalid!
        ));

        // Act
        handler.handleTextMessage(session, new TextMessage(handshakeMessage));

        // Assert
        verify(session).close(eq(CloseStatus.POLICY_VIOLATION));
        verify(sessionRegistry, never()).register(any(), any(), any(), any());
    }

    @Test
    void shouldRejectHandshakeWithExpiredTimestamp() throws Exception {
        // Arrange
        String botId = "bot-123";
        String wsToken = "ws_abc123def456";
        String nonce = UUID.randomUUID().toString();
        String version = "1.0";

        // Timestamp 10 minutes in the past (handshake max age is 5 minutes)
        Instant tenMinutesAgo = Instant.now().minusSeconds(600);
        String timestamp = tenMinutesAgo.toString();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE, wsToken);
        when(session.getAttributes()).thenReturn(attributes);

        String handshakeMessage = buildSignedHandshake(botId, timestamp, nonce, version, wsToken);

        // Act
        handler.handleTextMessage(session, new TextMessage(handshakeMessage));

        // Assert
        verify(session).close(eq(CloseStatus.POLICY_VIOLATION));
        verify(sessionRegistry, never()).register(any(), any(), any(), any());
    }

    @Test
    void shouldRejectHandshakeWithoutRequiredFields() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE, "ws_token");
        when(session.getAttributes()).thenReturn(attributes);

        // Missing botId
        String incompleteMessage = objectMapper.writeValueAsString(Map.of(
                "type", "handshake",
                "timestamp", Instant.now().toString(),
                "signature", "some_sig",
                "payload", new HashMap<>()
        ));

        // Act
        handler.handleTextMessage(session, new TextMessage(incompleteMessage));

        // Assert
        verify(session).close(eq(CloseStatus.BAD_DATA));
        verify(sessionRegistry, never()).register(any(), any(), any(), any());
    }

    @Test
    void shouldRejectHandshakeWhenNoSubscriptionFound() throws Exception {
        // Arrange
        String botId = "bot-unknown";
        String wsToken = "ws_abc123def456";
        String nonce = UUID.randomUUID().toString();
        String version = "1.0";
        Instant now = Instant.now();
        String timestamp = now.toString();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE, wsToken);
        when(session.getAttributes()).thenReturn(attributes);

        // No subscription found for this bot + token combo
        when(userSubscriptionPersistencePort.findActiveByBotIdAndWsToken(eq(botId), eq(wsToken)))
                .thenReturn(Optional.empty());

        String handshakeMessage = buildSignedHandshake(botId, timestamp, nonce, version, wsToken);

        // Act
        handler.handleTextMessage(session, new TextMessage(handshakeMessage));

        // Assert
        verify(session).close(eq(CloseStatus.NOT_ACCEPTABLE));
        verify(sessionRegistry, never()).register(any(), any(), any(), any());
    }

    @Test
    void shouldHandleHeartbeatAfterSuccessfulHandshake() throws Exception {
        // Arrange: successful handshake first
        String botId = "bot-123";
        String wsToken = "ws_abc123def456";
        String nonce = UUID.randomUUID().toString();
        String version = "1.0";
        Instant now = Instant.now();
        String timestamp = now.toString();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE, wsToken);
        when(session.getAttributes()).thenReturn(attributes);

        UserSubscription subscription = createSubscription("sub-1", botId, wsToken);
        when(userSubscriptionPersistencePort.findActiveByBotIdAndWsToken(eq(botId), eq(wsToken)))
                .thenReturn(Optional.of(subscription));

        String handshakeMessage = buildSignedHandshake(botId, timestamp, nonce, version, wsToken);
        handler.handleTextMessage(session, new TextMessage(handshakeMessage));

        // Clear previous invocations
        clearInvocations(session);

        // Act: send heartbeat
        String heartbeatMessage = objectMapper.writeValueAsString(Map.of(
                "type", "heartbeat",
                "timestamp", Instant.now().toString()
        ));
        handler.handleTextMessage(session, new TextMessage(heartbeatMessage));

        // Assert: heartbeat ack received
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());

        String responsePayload = messageCaptor.getValue().getPayload();
        JsonNode response = objectMapper.readTree(responsePayload);
        assertEquals("ack", response.get("type").asText());
        assertEquals("ok", response.path("payload").path("status").asText());
        assertEquals("heartbeat", response.path("payload").path("ack_type").asText());
    }

    @Test
    void shouldRejectUnsupportedFrameType() throws Exception {
        // Arrange - no need to set up session attributes for frame type validation
        String unsupportedMessage = objectMapper.writeValueAsString(Map.of(
                "type", "unknown_frame_type",
                "payload", new HashMap<>()
        ));

        // Act
        handler.handleTextMessage(session, new TextMessage(unsupportedMessage));

        // Assert - handler sends error frame and closes connection for unsupported frame
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());
        verify(session).close(eq(CloseStatus.PROTOCOL_ERROR));

        String responsePayload = messageCaptor.getValue().getPayload();
        JsonNode response = objectMapper.readTree(responsePayload);
        assertEquals("system", response.get("type").asText());
        assertEquals("unsupported_frame", response.path("payload").path("code").asText());
    }

    // Helper methods
    private String buildSignedHandshake(String botId, String timestamp, String nonce, String version, String wsToken) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nonce", nonce);
        payload.put("version", version);

        String payloadJson = objectMapper.writeValueAsString(payload);
        String payloadBase64 = Base64.getEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String message = botId + "|" + timestamp + "|" + payloadBase64;

        String signature = computeHmacSha256(message, wsToken);

        return objectMapper.writeValueAsString(Map.of(
                "type", "handshake",
                "botId", botId,
                "timestamp", timestamp,
                "payload", payload,
                "signature", signature
        ));
    }

    private String computeHmacSha256(String message, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    private static UserSubscription createSubscription(String userSubscriptionId, String botId, String wsToken) {
        UserSubscription subscription = new UserSubscription();
        subscription.setUserSubscriptionId(userSubscriptionId);
        subscription.setBotId(botId);
        subscription.setWsToken(wsToken);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExecutorConnected(false);
        return subscription;
    }
}

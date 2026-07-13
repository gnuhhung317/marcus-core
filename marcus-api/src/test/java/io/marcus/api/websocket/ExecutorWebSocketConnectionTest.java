package io.marcus.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.vo.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutorWebSocketConnectionTest {

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

    @Mock
    private io.marcus.domain.executor.ExecutionEventPort executionEventPort;

    @InjectMocks
    private ExecutorWebSocketHandler handler;

    @Test
    void shouldStoreWsTokenDuringHandshake() {
        ExecutorHandshakeInterceptor interceptor = new ExecutorHandshakeInterceptor();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/executor");
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ws-token-123");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                org.mockito.Mockito.mock(WebSocketHandler.class),
                attributes
        );

        assertTrue(accepted);
        assertEquals("ws-token-123", attributes.get(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE));
    }

    @Test
    void shouldRegisterExecutorAndAcknowledgeSubscribeOnMatchingToken() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ExecutorHandshakeInterceptor.WS_TOKEN_ATTRIBUTE, "ws-token-123");

        when(session.getAttributes()).thenReturn(attributes);
        when(userSubscriptionPersistencePort.findActiveByBotIdAndWsToken(eq("bot-1"), eq("ws-token-123")))
                .thenReturn(Optional.of(
                        createSubscription("subscription-1", "bot-1", "ws-token-123")
                ));

        handler.handleTextMessage(session, new TextMessage("""
                {
                  "type": "subscribe",
                  "payload": {
                    "bot_id": "bot-1"
                  }
                }
                """));

        verify(sessionRegistry).register("ws-token-123", "bot-1", "subscription-1", session);
        verify(userSubscriptionPersistencePort).markExecutorConnected("subscription-1", true);
        org.mockito.ArgumentCaptor<TextMessage> messageCaptor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());
        String payload = messageCaptor.getValue().getPayload();
        assertTrue(payload.contains("\"type\":\"ack\""));
        assertTrue(payload.contains("\"ack_type\":\"subscribe\""));
        assertTrue(payload.contains("\"status\":\"ok\""));
        assertTrue(payload.contains("\"bot_id\":\"bot-1\""));
    }

    @Test
    void shouldHandleReplayRequestAndSendReplayResponse() throws Exception {
        String signalId = "sig-123";
        int fromSequence = 0;
        String botId = "bot-1";

        java.time.Instant now = java.time.Instant.now();
        io.marcus.domain.executor.ExecutionEvent event = io.marcus.domain.executor.ExecutionEvent.create(
                "evt-1", signalId, 0, io.marcus.domain.executor.ExecutionEventType.SIGNAL_ACCEPTED,
                now, null, Map.of("status", "accepted")
        );

        when(executionEventPort.findBySignalIdAndSequenceRange(eq(signalId), eq(fromSequence), eq(Integer.MAX_VALUE)))
                .thenReturn(java.util.List.of(event));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("botId", botId);
        org.mockito.Mockito.lenient().when(session.getAttributes()).thenReturn(attributes);

        handler.handleTextMessage(session, new TextMessage("""
                {
                  "type": "replay-request",
                  "botId": "bot-1",
                  "payload": {
                    "signalId": "sig-123",
                    "fromSequence": 0
                  }
                }
                """));

        org.mockito.ArgumentCaptor<TextMessage> messageCaptor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());
        String payload = messageCaptor.getValue().getPayload();
        
        assertTrue(payload.contains("\"type\":\"replay-response\""));
        assertTrue(payload.contains("\"botId\":\"bot-1\""));
        assertTrue(payload.contains("\"signalId\":\"sig-123\""));
        assertTrue(payload.contains("\"events\""));
        assertTrue(payload.contains("\"eventId\":\"evt-1\""));
        assertTrue(payload.contains("\"sequence\":0"));
        assertTrue(payload.contains("\"eventType\":\"SIGNAL_ACCEPTED\""));
    }

    @Test
    void shouldRejectHandshakeWithoutBearerToken() {
        ExecutorHandshakeInterceptor interceptor = new ExecutorHandshakeInterceptor();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/executor");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                mock(WebSocketHandler.class),
                new HashMap<>()
        );

        assertFalse(accepted);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), servletResponse.getStatus());
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

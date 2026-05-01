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
    private WebSocketSession session;

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

        verify(sessionRegistry).register("bot-1", session);
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

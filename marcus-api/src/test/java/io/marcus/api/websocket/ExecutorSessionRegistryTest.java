package io.marcus.api.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutorSessionRegistryTest {

    @Test
    void shouldBroadcastMessageToRegisteredBotSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);

        ExecutorSessionRegistry registry = new ExecutorSessionRegistry();
        registry.register("bot-1", session);

        registry.broadcastToBot("bot-1", "{\"type\":\"signal\"}");

        verify(session).sendMessage(any(TextMessage.class));
    }
}
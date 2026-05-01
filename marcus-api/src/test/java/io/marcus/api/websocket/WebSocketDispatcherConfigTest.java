package io.marcus.api.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketDispatcherConfigTest {

    @Mock
    private ExecutorWebSocketHandler executorWebSocketHandler;

    @Mock
    private ExecutorHandshakeInterceptor executorHandshakeInterceptor;

    @Mock
    private WebSocketHandlerRegistry registry;

    @Mock
    private WebSocketHandlerRegistration registration;

    @InjectMocks
    private WebSocketDispatcherConfig webSocketDispatcherConfig;

    @Test
    void shouldRegisterExecutorEndpointWithHandshakeInterceptor() {
        when(registry.addHandler(executorWebSocketHandler, "/ws/executor")).thenReturn(registration);
        when(registration.addInterceptors(executorHandshakeInterceptor)).thenReturn(registration);
        when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

        webSocketDispatcherConfig.registerWebSocketHandlers(registry);

        verify(registry).addHandler(eq(executorWebSocketHandler), eq("/ws/executor"));
        verify(registration).addInterceptors(executorHandshakeInterceptor);
        verify(registration).setAllowedOriginPatterns("*");
    }
}

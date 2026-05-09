package io.marcus.api.config;

import io.marcus.api.controller.executor.ExecutorWsHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Spring WebSocket configuration for executor ingest channel. Registers the
 * ExecutorWsHandler on the /ws/executor endpoint.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ExecutorWsHandler executorWsHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(executorWsHandler, "/ws/executor")
                .setAllowedOrigins("*") // TODO: Configure CORS for production
                .withSockJS();  // Enable SockJS fallback for browsers
    }
}

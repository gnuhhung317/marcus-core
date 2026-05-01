package io.marcus.api.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableKafka
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketDispatcherConfig implements WebSocketConfigurer {

    private final ExecutorWebSocketHandler executorWebSocketHandler;
    private final ExecutorHandshakeInterceptor executorHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(executorWebSocketHandler, "/ws/executor")
                .addInterceptors(executorHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}

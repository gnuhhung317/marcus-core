package io.marcus.infrastructure.security.config;

import io.marcus.infrastructure.security.BotSignatureInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BotSignatureWebMvcConfigurer implements WebMvcConfigurer {

    private final BotSignatureInterceptor botSignatureInterceptor;

    public BotSignatureWebMvcConfigurer(BotSignatureInterceptor botSignatureInterceptor) {
        this.botSignatureInterceptor = botSignatureInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(botSignatureInterceptor)
                .addPathPatterns(
                        "/signal/**",
                        "/signals/**",
                        "/api/signals/**",
                        "/api/v1/signals/**",
                        "/routing/**",
                        "/api/v1/routing/**",
                        "/bots/*/backtest-results",
                        "/api/bots/*/backtest-results",
                        "/api/v1/bots/*/backtest-results",
                        "/bots/*/dry-run/**",
                        "/api/bots/*/dry-run/**",
                        "/api/v1/bots/*/dry-run/**",
                        "/bots/*/telemetry/**",
                        "/api/bots/*/telemetry/**",
                        "/api/v1/bots/*/telemetry/**",
                        "/bots/*/heartbeat",
                        "/api/bots/*/heartbeat",
                        "/api/v1/bots/*/heartbeat"
                );
    }
}

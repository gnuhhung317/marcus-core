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
                .addPathPatterns("/signal/**", "/api/v1/signals/**");
    }
}

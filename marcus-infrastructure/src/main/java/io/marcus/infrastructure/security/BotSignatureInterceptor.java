package io.marcus.infrastructure.security;

import io.marcus.infrastructure.crypto.HmacSignatureValidator;
import io.marcus.infrastructure.security.wrapper.MultiReadHttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotSignatureInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;
    private final HmacSignatureValidator hmacSignatureValidator;
    private final BotSecretProvider botSecretProvider;
    private static final int IDEMPOTENCY_WINDOW_SECONDS = 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. Check if the endpoint even has our annotation
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            if (!handlerMethod.hasMethodAnnotation(RequireBotSignature.class)) {
                return true; // No tag? Let it pass immediately.
            }
        }

        // 2. Extract Headers: X-Timestamp, X-Bot-Api-Key, X-Signature
        String timestampHeader = request.getHeader("X-Timestamp");
        String apiKey = request.getHeader("X-Bot-Api-Key");
        String signature = request.getHeader("X-Signature");

        if (timestampHeader == null || apiKey == null || signature == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        long timestamp = Long.parseLong(timestampHeader);

        // 3. Prevent Replay Attack
        if (timestamp < System.currentTimeMillis() - 5000) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        // 4. Redis Idempotency Check
        String redisKey = "idem:sig:" + signature;
        Boolean isUnique = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(IDEMPOTENCY_WINDOW_SECONDS));
        if (Boolean.FALSE.equals(isUnique)) {
            log.warn("Idempotency Violation! Signature: {}", signature);
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return false;
        }

        String jsonBody;
        // 5. Read body from our cached wrapper
        if (request instanceof MultiReadHttpServletRequestWrapper multiReadRequest) {
            jsonBody = multiReadRequest.getBody();
        } else {
            log.error("Can not read request body");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }

        // 6. Calculate HMAC-SHA256
        String botSecret = botSecretProvider.getEncryptedSecret(apiKey);
        if (botSecret == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }

        boolean isValid = hmacSignatureValidator.isValid(jsonBody, botSecret, signature);
        if (!isValid) {
            log.warn("Invalid signature for API Key: {}", apiKey);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        return true;
    }
}

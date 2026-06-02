package io.marcus.infrastructure.security;

import io.marcus.infrastructure.crypto.HmacSignatureValidator;
import io.marcus.infrastructure.security.filter.RequestCachingFilter;
import io.marcus.infrastructure.security.wrapper.MultiReadHttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Locale;

@Component
public class BotSignatureInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(BotSignatureInterceptor.class);
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_API_KEY = "X-Bot-Api-Key";
    private static final String HEADER_SIGNATURE = "X-Signature";
    private static final int IDEMPOTENCY_WINDOW_SECONDS = 60;

    @Value("${marcus.security.bot-signature.max-skew-ms:60000}")
    private long maxTimestampSkewMillis = 60000;

    private final StringRedisTemplate redisTemplate;
    private final HmacSignatureValidator hmacSignatureValidator;
    private final BotSecretProvider botSecretProvider;

    public BotSignatureInterceptor(
            StringRedisTemplate redisTemplate,
            HmacSignatureValidator hmacSignatureValidator,
            BotSecretProvider botSecretProvider
    ) {
        this.redisTemplate = redisTemplate;
        this.hmacSignatureValidator = hmacSignatureValidator;
        this.botSecretProvider = botSecretProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        if (!handlerMethod.hasMethodAnnotation(RequireBotSignature.class)) {
            return true;
        }

        String timestampHeader = request.getHeader(HEADER_TIMESTAMP);
        String apiKey = request.getHeader(HEADER_API_KEY);
        String signatureHeader = request.getHeader(HEADER_SIGNATURE);

        if (timestampHeader == null || apiKey == null || signatureHeader == null) {
            log.warn("[BotSignatureReject] reason=missing_header, path={}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        timestampHeader = timestampHeader.trim();
        apiKey = apiKey.trim();
        String normalizedSignature = signatureHeader.trim().toLowerCase(Locale.ROOT);

        if (timestampHeader.isEmpty() || apiKey.isEmpty() || normalizedSignature.isEmpty()) {
            log.warn("[BotSignatureReject] reason=missing_header, path={}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException ex) {
            log.warn("[BotSignatureReject] reason=invalid_timestamp, timestampHeader={}, path={}", timestampHeader, request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        long now = System.currentTimeMillis();
        long skewMillis = Math.abs(now - timestamp);
        if (skewMillis > maxTimestampSkewMillis) {
            log.warn("[BotSignatureReject] reason=skew, apiKey={}, skewMillis={}, maxSkewMillis={}, path={}", apiKey, skewMillis, maxTimestampSkewMillis, request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        String redisKey = "idem:sig:" + apiKey + ":" + normalizedSignature;
        Boolean isUnique;
        try {
            isUnique = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(IDEMPOTENCY_WINDOW_SECONDS));
        } catch (Exception ex) {
            log.warn("Idempotency check failed due to Redis error: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return false;
        }

        if (Boolean.FALSE.equals(isUnique)) {
            log.warn("[BotSignatureReject] reason=duplicate_signature, apiKey={}, signature={}, path={}", apiKey, normalizedSignature, request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            return false;
        }

        String jsonBody = RequestCachingFilter.currentRequestBody();
        if (jsonBody == null) {
            jsonBody = (String) request.getAttribute(RequestCachingFilter.CACHED_REQUEST_BODY_ATTRIBUTE);
        }
        if (jsonBody == null && request instanceof MultiReadHttpServletRequestWrapper multiReadRequest) {
            jsonBody = multiReadRequest.getBody();
        }
        if (jsonBody == null && "GET".equalsIgnoreCase(request.getMethod())) {
            jsonBody = "{}";
        }
        if (jsonBody == null) {
            log.error("Can not read request body");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }

        String botSecret;
        try {
            botSecret = botSecretProvider.getEncryptedSecret(apiKey);
        } catch (Exception ex) {
            log.warn("[BotSignatureReject] reason=unknown_api_key, apiKey={}, path={}", apiKey, request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String signaturePayload = timestampHeader + "\n" + jsonBody;
        boolean isValid = hmacSignatureValidator.isValid(signaturePayload, botSecret, normalizedSignature);
        if (!isValid) {
            log.warn("[BotSignatureReject] reason=invalid_signature, apiKey={}, path={}", apiKey, request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        return true;
    }
}

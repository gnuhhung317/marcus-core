package io.marcus.infrastructure.security;

import io.marcus.infrastructure.crypto.HmacSignatureValidator;
import io.marcus.infrastructure.security.wrapper.MultiReadHttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotSignatureInterceptorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HmacSignatureValidator hmacSignatureValidator;

    @Mock
    private BotSecretProvider botSecretProvider;

    @InjectMocks
    private BotSignatureInterceptor interceptor;

    @Test
    void shouldAllowWhenHeadersSignatureAndIdempotencyAreValid() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String apiKey = "ak_test";
        String signature = "ABCDEF123456";

        HttpServletRequest request = createWrappedRequest(timestamp, apiKey, signature, "{\"signal\":\"BUY\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(String.class), eq("1"), any(Duration.class))).thenReturn(true);
        when(botSecretProvider.getEncryptedSecret(apiKey)).thenReturn("enc:secret");
        when(hmacSignatureValidator.isValid(any(String.class), eq("enc:secret"), eq(signature.toLowerCase()))).thenReturn(true);

        boolean allowed = interceptor.preHandle(request, response, createHandlerMethod());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);

        verify(hmacSignatureValidator).isValid(
                eq(timestamp + "\n" + "{\"signal\":\"BUY\"}"),
                eq("enc:secret"),
                eq(signature.toLowerCase())
        );
    }

    @Test
    void shouldRejectWhenMissingRequiredHeaders() throws Exception {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest();
        rawRequest.setMethod("POST");
        rawRequest.setContentType("application/json");
        rawRequest.setContent("{\"signal\":\"BUY\"}".getBytes(StandardCharsets.UTF_8));
        HttpServletRequest request = new MultiReadHttpServletRequestWrapper(rawRequest);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, createHandlerMethod());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(botSecretProvider, hmacSignatureValidator);
    }

    @Test
    void shouldRejectDuplicateSignatureEvenWhenCaseDiffers() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String apiKey = "ak_test";
        String uppercaseSignature = "ABCDEF123456";

        HttpServletRequest request = createWrappedRequest(timestamp, apiKey, uppercaseSignature, "{\"signal\":\"BUY\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(String.class), eq("1"), any(Duration.class))).thenReturn(false);

        boolean allowed = interceptor.preHandle(request, response, createHandlerMethod());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(409);

        ArgumentCaptor<String> redisKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).setIfAbsent(redisKeyCaptor.capture(), eq("1"), any(Duration.class));
        assertThat(redisKeyCaptor.getValue()).contains(uppercaseSignature.toLowerCase());
    }

    private HttpServletRequest createWrappedRequest(String timestamp, String apiKey, String signature, String body)
            throws Exception {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest();
        rawRequest.setMethod("POST");
        rawRequest.setContentType("application/json");
        rawRequest.addHeader("X-Timestamp", timestamp);
        rawRequest.addHeader("X-Bot-Api-Key", apiKey);
        rawRequest.addHeader("X-Signature", signature);
        rawRequest.setContent(body.getBytes(StandardCharsets.UTF_8));
        return new MultiReadHttpServletRequestWrapper(rawRequest);
    }

    private HandlerMethod createHandlerMethod() throws NoSuchMethodException {
        TestController controller = new TestController();
        return new HandlerMethod(controller, TestController.class.getMethod("handle"));
    }

    private static final class TestController {

        public void handle() {
        }
    }
}

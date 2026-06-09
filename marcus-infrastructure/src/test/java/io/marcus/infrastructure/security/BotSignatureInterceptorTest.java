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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    void shouldRejectWhenTimestampSkewExceedsLimit() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() - 61_000L);
        String apiKey = "ak_test";
        String signature = "ABCDEF123456";

        HttpServletRequest request = createWrappedRequest(timestamp, apiKey, signature, "{\"signal\":\"BUY\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, createHandlerMethod());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(redisTemplate, botSecretProvider, hmacSignatureValidator);
    }

    @Test
    void shouldValidateGzipRequestAgainstCompressedBytes() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String apiKey = "ak_test";
        String signature = "ABCDEF123456";
        String body = "{\"signal\":\"BUY\"}";
        byte[] compressedBody = gzip(body);

        HttpServletRequest request = createWrappedGzipRequest(timestamp, apiKey, signature, compressedBody);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(String.class), eq("1"), any(Duration.class))).thenReturn(true);
        when(botSecretProvider.getEncryptedSecret(apiKey)).thenReturn("enc:secret");
        when(hmacSignatureValidator.isValid(any(byte[].class), eq("enc:secret"), eq(signature.toLowerCase()))).thenReturn(true);

        boolean allowed = interceptor.preHandle(request, response, createHandlerMethod());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);

        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(hmacSignatureValidator).isValid(payloadCaptor.capture(), eq("enc:secret"), eq(signature.toLowerCase()));
        byte[] expectedPayload = new byte[timestamp.getBytes(StandardCharsets.UTF_8).length + 1 + compressedBody.length];
        System.arraycopy(timestamp.getBytes(StandardCharsets.UTF_8), 0, expectedPayload, 0, timestamp.getBytes(StandardCharsets.UTF_8).length);
        expectedPayload[timestamp.getBytes(StandardCharsets.UTF_8).length] = (byte) '\n';
        System.arraycopy(compressedBody, 0, expectedPayload, timestamp.getBytes(StandardCharsets.UTF_8).length + 1, compressedBody.length);
        assertThat(payloadCaptor.getValue()).isEqualTo(expectedPayload);
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

    private HttpServletRequest createWrappedGzipRequest(String timestamp, String apiKey, String signature, byte[] body)
            throws Exception {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest();
        rawRequest.setMethod("POST");
        rawRequest.setContentType("application/json");
        rawRequest.addHeader("Content-Encoding", "gzip");
        rawRequest.addHeader("X-Timestamp", timestamp);
        rawRequest.addHeader("X-Bot-Api-Key", apiKey);
        rawRequest.addHeader("X-Signature", signature);
        rawRequest.setContent(body);
        return new MultiReadHttpServletRequestWrapper(rawRequest);
    }

    private byte[] gzip(String value) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            gzipOutputStream.write(value.getBytes(StandardCharsets.UTF_8));
        }
        return outputStream.toByteArray();
    }

    private HandlerMethod createHandlerMethod() throws NoSuchMethodException {
        TestController controller = new TestController();
        return new HandlerMethod(controller, TestController.class.getMethod("handle"));
    }

    private static final class TestController {

        @RequireBotSignature
        public void handle() {
        }
    }
}

package io.marcus.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCachingFilterTest {

    @Test
    void shouldReturnBadRequestWhenGzipBodyIsInvalid() throws Exception {
        RequestCachingFilter filter = new RequestCachingFilter(1024);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json");
        request.addHeader("Content-Encoding", "gzip");
        request.setContent("not-gzip".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, markingChain(chainCalled));

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getErrorMessage()).contains("Invalid gzip request body");
    }

    @Test
    void shouldReturnPayloadTooLargeWhenCompressedBodyExceedsLimit() throws Exception {
        RequestCachingFilter filter = new RequestCachingFilter(8);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json");
        request.setContent("0123456789".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, markingChain(chainCalled));

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getErrorMessage()).contains("Compressed request body exceeds limit");
    }

    @Test
    void shouldPassDecompressedBodyToDownstreamHandlers() throws Exception {
        RequestCachingFilter filter = new RequestCachingFilter(4096);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json");
        request.addHeader("Content-Encoding", "gzip");
        request.setContent(gzip("{\"hello\":\"world\"}"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (wrappedRequest, wrappedResponse) -> {
            chainCalled.set(true);
            assertThat(RequestCachingFilter.currentRawRequestBody()).isNotNull();
            assertThat(RequestCachingFilter.currentRequestBody()).isEqualTo("{\"hello\":\"world\"}");
            assertThat((String) wrappedRequest.getAttribute(RequestCachingFilter.CACHED_REQUEST_BODY_ATTRIBUTE))
                    .isEqualTo("{\"hello\":\"world\"}");
            assertThat((Boolean) wrappedRequest.getAttribute(RequestCachingFilter.CACHED_GZIP_ENCODED_ATTRIBUTE))
                    .isTrue();
        });

        assertThat(chainCalled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private static FilterChain markingChain(AtomicBoolean chainCalled) {
        return (request, response) -> chainCalled.set(true);
    }

    private static byte[] gzip(String value) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            gzipOutputStream.write(value.getBytes(StandardCharsets.UTF_8));
        }
        return outputStream.toByteArray();
    }
}

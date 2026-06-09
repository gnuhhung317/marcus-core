package io.marcus.infrastructure.security.filter;

import io.marcus.infrastructure.security.wrapper.MultiReadHttpServletRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCachingFilter extends OncePerRequestFilter {

    public static final String CACHED_REQUEST_BODY_ATTRIBUTE = "marcus.cachedRequestBody";
    public static final String CACHED_RAW_REQUEST_BODY_ATTRIBUTE = "marcus.cachedRawRequestBody";
    public static final String CACHED_GZIP_ENCODED_ATTRIBUTE = "marcus.cachedGzipEncoded";
    private static final ThreadLocal<String> CACHED_REQUEST_BODY = new ThreadLocal<>();
    private static final ThreadLocal<byte[]> CACHED_RAW_REQUEST_BODY = new ThreadLocal<>();
    private final int maxCompressedRequestBytes;

    public RequestCachingFilter(int maxCompressedRequestBytes) {
        this.maxCompressedRequestBytes = maxCompressedRequestBytes;
    }

    public static String currentRequestBody() {
        return CACHED_REQUEST_BODY.get();
    }

    public static byte[] currentRawRequestBody() {
        return CACHED_RAW_REQUEST_BODY.get();
    }

    public static void clearCurrentRequestBody() {
        CACHED_REQUEST_BODY.remove();
        CACHED_RAW_REQUEST_BODY.remove();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        MultiReadHttpServletRequestWrapper wrappedRequest;
        try {
            wrappedRequest = new MultiReadHttpServletRequestWrapper(request, maxCompressedRequestBytes);
        } catch (MultiReadHttpServletRequestWrapper.RequestBodyTooLargeException ex) {
            response.sendError(
                    HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Compressed request body exceeds limit of " + ex.getMaxAllowedBytes() + " bytes"
            );
            return;
        } catch (MultiReadHttpServletRequestWrapper.InvalidGzipBodyException ex) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid gzip request body for Content-Encoding: gzip"
            );
            return;
        }

        CACHED_REQUEST_BODY.set(wrappedRequest.getBody());
        CACHED_RAW_REQUEST_BODY.set(wrappedRequest.getRawBody());
        wrappedRequest.setAttribute(CACHED_REQUEST_BODY_ATTRIBUTE, wrappedRequest.getBody());
        wrappedRequest.setAttribute(CACHED_RAW_REQUEST_BODY_ATTRIBUTE, wrappedRequest.getRawBody());
        wrappedRequest.setAttribute(CACHED_GZIP_ENCODED_ATTRIBUTE, wrappedRequest.isGzipEncoded());

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            clearCurrentRequestBody();
        }
    }
}

package io.marcus.infrastructure.security.filter;

import io.marcus.infrastructure.security.wrapper.MultiReadHttpServletRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCachingFilter extends OncePerRequestFilter {

    public static final String CACHED_REQUEST_BODY_ATTRIBUTE = "marcus.cachedRequestBody";
    private static final ThreadLocal<String> CACHED_REQUEST_BODY = new ThreadLocal<>();

    public static String currentRequestBody() {
        return CACHED_REQUEST_BODY.get();
    }

    public static void clearCurrentRequestBody() {
        CACHED_REQUEST_BODY.remove();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // wrap the request with custom wrapper for reading the body multiple times
        MultiReadHttpServletRequestWrapper wrappedRequest = new MultiReadHttpServletRequestWrapper(request);
        CACHED_REQUEST_BODY.set(wrappedRequest.getBody());
        wrappedRequest.setAttribute(CACHED_REQUEST_BODY_ATTRIBUTE, wrappedRequest.getBody());

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            clearCurrentRequestBody();
        }
    }
}

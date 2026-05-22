package io.marcus.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        log.warn("Unauthorized request to path={}: {}", request.getRequestURI(), authException.getMessage());

        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        ErrorResponse errorResponse = new ErrorResponse(
                "UNAUTHORIZED",
                authException.getMessage() != null ? authException.getMessage() : "Full authentication is required to access this resource",
                HttpStatus.UNAUTHORIZED.value(),
                traceId,
                request.getRequestURI(),
                Instant.now()
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("X-Trace-Id", traceId);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}

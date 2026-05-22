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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        log.warn("Access denied to path={}: {}", request.getRequestURI(), accessDeniedException.getMessage());

        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        ErrorResponse errorResponse = new ErrorResponse(
                "FORBIDDEN",
                accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "Access is denied",
                HttpStatus.FORBIDDEN.value(),
                traceId,
                request.getRequestURI(),
                Instant.now()
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader("X-Trace-Id", traceId);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}

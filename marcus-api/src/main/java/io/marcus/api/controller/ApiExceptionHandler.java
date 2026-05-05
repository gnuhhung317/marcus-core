package io.marcus.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Bad request");
        body.put("code", "BAD_REQUEST");
        body.put("message", ex.getMessage());
        body.put("traceId", traceId);
        log.warn("Bad request (IllegalArgumentException): {} | traceId={}", ex.getMessage(), traceId, ex);
        return ResponseEntity.badRequest()
                .header("X-Trace-Id", traceId)
                .body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Bad request");
        body.put("code", "MISSING_PARAMETER");
        body.put("message", String.format("Missing required parameter: %s", ex.getParameterName()));
        body.put("traceId", traceId);
        log.warn("Missing parameter: {} | paramType={} | traceId={}", ex.getParameterName(), ex.getParameterType(), traceId);
        return ResponseEntity.badRequest()
                .header("X-Trace-Id", traceId)
                .body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Forbidden");
        body.put("code", "FORBIDDEN");
        body.put("message", ex.getMessage());
        body.put("traceId", traceId);
        log.warn("Access denied: {} | traceId={}", ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("X-Trace-Id", traceId)
                .body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Method not allowed");
        body.put("code", "METHOD_NOT_ALLOWED");
        body.put("message", ex.getMessage());
        body.put("traceId", traceId);
        log.warn("Method not allowed: {} | supported={} | traceId={}", ex.getMethod(), ex.getSupportedMethods(), traceId);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, String.join(", ", ex.getSupportedMethods()))
                .header("X-Trace-Id", traceId)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnhandled(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Internal server error");
        body.put("code", "INTERNAL_SERVER_ERROR");
        body.put("message", ex.getMessage());
        body.put("traceId", traceId);
        log.error("Unhandled exception in ApiExceptionHandler | exceptionType={} | traceId={}",
                ex.getClass().getSimpleName(), traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Trace-Id", traceId)
                .body(body);
    }
}

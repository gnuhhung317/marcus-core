package io.marcus.api.controller;

import io.marcus.application.exception.ResourceConflictException;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.exception.BotNotFoundException;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Bad request", ex.getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "MISSING_PARAMETER",
            "Bad request",
            String.format("Missing required parameter: %s", ex.getParameterName()),
            request
        );
    }

        @ExceptionHandler(ResourceConflictException.class)
        public ResponseEntity<Map<String, Object>> handleResourceConflict(ResourceConflictException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "CONFLICT", "Conflict", ex.getMessage(), request);
        }

    @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, Object>> handleForbiddenOperation(ForbiddenOperationException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticated(UnauthenticatedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized", ex.getMessage(), request);
    }

    @ExceptionHandler(BotNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBotNotFound(BotNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "BOT_NOT_FOUND", "Not found", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        Map<String, Object> response = buildBaseErrorBody(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "Method not allowed",
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .header(HttpHeaders.ALLOW, String.join(", ", ex.getSupportedMethods()))
            .header("X-Trace-Id", (String) response.get("traceId"))
            .body(response);
    }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
        ) {
        String traceId = resolveTraceId();
        Map<String, Object> body = buildBaseErrorBody(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "VALIDATION_FAILED",
            "Validation failed",
            ex.getMessage(),
            request,
            traceId
        );
        body.put("errors", ex.getBindingResult().getFieldErrors().stream().map(fieldError -> Map.of(
            "field", fieldError.getField(),
            "reason", fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage()
        )).toList());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .header("X-Trace-Id", traceId)
            .body(body);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
        ) {
        String traceId = resolveTraceId();
        Map<String, Object> body = buildBaseErrorBody(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "VALIDATION_FAILED",
            "Validation failed",
            ex.getMessage(),
            request,
            traceId
        );
        body.put("errors", ex.getConstraintViolations().stream().map(violation -> Map.of(
            "field", violation.getPropertyPath().toString(),
            "reason", violation.getMessage()
        )).toList());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .header("X-Trace-Id", traceId)
            .body(body);
        }

    @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleUnhandled(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId();
        Map<String, Object> body = buildBaseErrorBody(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "Internal server error",
            ex.getMessage(),
            request,
            traceId
        );
        log.error("Unhandled exception in ApiExceptionHandler | exceptionType={} | traceId={}",
            ex.getClass().getSimpleName(), traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("X-Trace-Id", traceId)
            .body(body);
    }

        private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String code,
            String error,
            String message,
            HttpServletRequest request
        ) {
        String traceId = resolveTraceId();
        Map<String, Object> body = buildBaseErrorBody(status, code, error, message, request, traceId);
        return ResponseEntity.status(status)
            .header("X-Trace-Id", traceId)
            .body(body);
        }

        private Map<String, Object> buildBaseErrorBody(
            HttpStatus status,
            String code,
            String error,
            String message,
            HttpServletRequest request
        ) {
        return buildBaseErrorBody(status, code, error, message, request, resolveTraceId());
        }

        private Map<String, Object> buildBaseErrorBody(
            HttpStatus status,
            String code,
            String error,
            String message,
            HttpServletRequest request,
            String traceId
        ) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("code", code);
        body.put("message", message);
        body.put("status", status.value());
        body.put("traceId", traceId);
        body.put("path", request.getRequestURI());
        body.put("timestamp", Instant.now());
        return body;
        }

        private String resolveTraceId() {
        return UUID.randomUUID().toString();
        }
}

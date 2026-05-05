package io.marcus.api.exception;

import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.ResourceConflictException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.exception.BotNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionsHandler {

    @ExceptionHandler(BotNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBotNotFound(BotNotFoundException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("Bot not found: {} | traceId={} | path={}", ex.getMessage(), traceId, request.getRequestURI());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "BOT_NOT_FOUND", ex.getMessage(), request, traceId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("Bad request (IllegalArgumentException): {} | traceId={} | method={} | path={} | requestURI={}", 
                ex.getMessage(), traceId, request.getMethod(), request.getRequestURI(), request.getRequestURL(), ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, traceId);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("Resource not found: {} | traceId={} | path={}", ex.getMessage(), traceId, request.getRequestURI());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, traceId);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflict(ResourceConflictException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("Resource conflict: {} | traceId={} | path={}", ex.getMessage(), traceId, request.getRequestURI());
        return buildErrorResponse(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request, traceId);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticated(UnauthenticatedException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("Unauthenticated request: {} | traceId={} | path={}", ex.getMessage(), traceId, request.getRequestURI());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request, traceId);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenOperation(ForbiddenOperationException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("Forbidden operation: {} | traceId={} | path={}", ex.getMessage(), traceId, request.getRequestURI());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), request, traceId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        List<FieldValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new FieldValidationError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage()
                ))
                .toList();

        log.warn("Validation failed: {} errors | traceId={} | method={} | path={} | fields={}",
                errors.size(), traceId, request.getMethod(), request.getRequestURI(),
                errors.stream().map(FieldValidationError::field).toList());

        return buildValidationErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDATION_FAILED",
                "Validation failed",
                errors,
                request,
                traceId
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        List<FieldValidationError> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new FieldValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        log.warn("Constraint violation: {} violations | traceId={} | method={} | path={}",
                errors.size(), traceId, request.getMethod(), request.getRequestURI(), ex);

        return buildValidationErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDATION_FAILED",
                "Validation failed",
                errors,
                request,
                traceId
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        String message = "Malformed JSON request body or invalid request format";
        log.warn("HTTP message not readable (400 Bad Request): {} | traceId={} | method={} | path={} | cause={}",
                message, traceId, request.getMethod(), request.getRequestURI(),
                ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(), ex);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                message,
                request,
                traceId
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        String message = String.format("Invalid value '%s' for parameter '%s': expected type %s",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
        log.warn("Method argument type mismatch (400 Bad Request): {} | traceId={} | method={} | path={}",
                message, traceId, request.getMethod(), request.getRequestURI(), ex);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER_TYPE",
                message,
                request,
                traceId
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error("Unhandled exception occurred | traceId={} | method={} | path={} | exceptionType={}",
                traceId, request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName(), ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Internal server error",
                request,
                traceId
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return buildErrorResponse(status, code, message, request, resolveTraceId(request));
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            String traceId
    ) {
        ErrorResponse response = new ErrorResponse(
                code,
                message,
                status.value(),
                traceId,
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity.status(status)
                .header("X-Trace-Id", response.traceId())
                .body(response);
    }

    private ResponseEntity<ValidationErrorResponse> buildValidationErrorResponse(
            HttpStatus status,
            String code,
            String message,
            List<FieldValidationError> errors,
            HttpServletRequest request
    ) {
        return buildValidationErrorResponse(status, code, message, errors, request, resolveTraceId(request));
    }

    private ResponseEntity<ValidationErrorResponse> buildValidationErrorResponse(
            HttpStatus status,
            String code,
            String message,
            List<FieldValidationError> errors,
            HttpServletRequest request,
            String traceId
    ) {
        ValidationErrorResponse response = new ValidationErrorResponse(
                code,
                message,
                status.value(),
                traceId,
                request.getRequestURI(),
                Instant.now(),
                errors
        );

        return ResponseEntity.status(status)
                .header("X-Trace-Id", response.traceId())
                .body(response);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }
}

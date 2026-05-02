package io.marcus.api.exception;

import java.time.Instant;
import java.util.List;

public record ValidationErrorResponse(
        String code,
        String message,
        int status,
        String traceId,
        String path,
        Instant timestamp,
        List<FieldValidationError> errors
) {
}
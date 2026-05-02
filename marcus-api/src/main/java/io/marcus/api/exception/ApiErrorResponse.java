package io.marcus.api.exception;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        String traceId,
        String path,
        Instant timestamp
) {
}
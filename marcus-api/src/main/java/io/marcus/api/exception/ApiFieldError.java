package io.marcus.api.exception;

public record ApiFieldError(
        String field,
        String reason
) {
}
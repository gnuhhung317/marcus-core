package io.marcus.api.exception;

public record FieldValidationError(
        String field,
        String reason
) {
}
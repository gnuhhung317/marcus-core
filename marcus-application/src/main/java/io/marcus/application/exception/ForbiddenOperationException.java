package io.marcus.application.exception;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
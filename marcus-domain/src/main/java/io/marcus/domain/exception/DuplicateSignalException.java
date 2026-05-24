package io.marcus.domain.exception;

/**
 * Thrown when a signal with the same {@code signalId} is submitted more than once.
 * Signals an HTTP 409 Conflict to the caller.
 */
public class DuplicateSignalException extends ResourceConflictException {

    public DuplicateSignalException(String signalId) {
        super("Signal already exists: " + signalId);
    }
}

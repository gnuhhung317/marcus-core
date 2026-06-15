package io.marcus.domain.exception;

public class KycDocumentNotFoundException extends RuntimeException {
    public KycDocumentNotFoundException(String message) {
        super(message);
    }
}

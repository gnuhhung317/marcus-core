package io.marcus.application.dto;

import io.marcus.domain.vo.KycDocumentType;

public record KycUploadInitiateRequest(
        KycDocumentType documentType,
        String contentType
) {
}

package io.marcus.application.dto;

import io.marcus.domain.vo.KycDocumentType;
import io.marcus.domain.vo.KycStatus;

import java.time.LocalDateTime;

public record KycDocumentResponse(
        String documentId,
        String userId,
        KycDocumentType documentType,
        String objectKey,
        KycStatus status,
        Long fileSize,
        String contentType,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

package io.marcus.infrastructure.persistence.mapper;

import io.marcus.domain.model.KycDocument;
import io.marcus.infrastructure.persistence.entity.KycDocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class KycDocumentMapper {

    public KycDocument toDomain(KycDocumentEntity entity) {
        if (entity == null) {
            return null;
        }

        return KycDocument.builder()
                .documentId(entity.getDocumentId())
                .userId(entity.getUserId())
                .documentType(entity.getDocumentType())
                .objectKey(entity.getObjectKey())
                .fileSize(entity.getFileSize())
                .contentType(entity.getContentType())
                .status(entity.getStatus())
                .rejectReason(entity.getRejectReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public KycDocumentEntity toEntity(KycDocument domain) {
        if (domain == null) {
            return null;
        }

        return KycDocumentEntity.builder()
                .documentId(domain.getDocumentId())
                .userId(domain.getUserId())
                .documentType(domain.getDocumentType())
                .objectKey(domain.getObjectKey())
                .fileSize(domain.getFileSize())
                .contentType(domain.getContentType())
                .status(domain.getStatus())
                .rejectReason(domain.getRejectReason())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}

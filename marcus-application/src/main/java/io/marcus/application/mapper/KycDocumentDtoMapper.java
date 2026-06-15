package io.marcus.application.mapper;

import io.marcus.application.dto.KycDocumentResponse;
import io.marcus.domain.model.KycDocument;
import org.springframework.stereotype.Component;

@Component
public class KycDocumentDtoMapper {

    public KycDocumentResponse toResponse(KycDocument doc) {
        if (doc == null) {
            return null;
        }
        return new KycDocumentResponse(
                doc.getDocumentId(),
                doc.getUserId(),
                doc.getDocumentType(),
                doc.getObjectKey(),
                doc.getStatus(),
                doc.getFileSize(),
                doc.getContentType(),
                doc.getRejectReason(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}

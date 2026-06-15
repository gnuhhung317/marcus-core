package io.marcus.domain.repository;

import io.marcus.domain.model.KycDocument;
import io.marcus.domain.vo.KycDocumentType;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository {
    KycDocument save(KycDocument kycDocument);
    Optional<KycDocument> findById(String documentId);
    List<KycDocument> findByUserId(String userId);
    Optional<KycDocument> findByUserIdAndDocumentType(String userId, KycDocumentType documentType);
}

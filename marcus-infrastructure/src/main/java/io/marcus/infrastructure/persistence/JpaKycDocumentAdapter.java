package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.KycDocument;
import io.marcus.domain.repository.KycDocumentRepository;
import io.marcus.domain.vo.KycDocumentType;
import io.marcus.infrastructure.persistence.entity.KycDocumentEntity;
import io.marcus.infrastructure.persistence.mapper.KycDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaKycDocumentAdapter implements KycDocumentRepository {

    private final SpringDataKycDocumentRepository springDataKycDocumentRepository;
    private final KycDocumentMapper kycDocumentMapper;

    @Override
    public KycDocument save(KycDocument kycDocument) {
        KycDocumentEntity entity = kycDocumentMapper.toEntity(kycDocument);
        KycDocumentEntity savedEntity = springDataKycDocumentRepository.save(entity);
        return kycDocumentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<KycDocument> findById(String documentId) {
        return springDataKycDocumentRepository.findById(documentId)
                .map(kycDocumentMapper::toDomain);
    }

    @Override
    public List<KycDocument> findByUserId(String userId) {
        return springDataKycDocumentRepository.findByUserId(userId).stream()
                .map(kycDocumentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<KycDocument> findByUserIdAndDocumentType(String userId, KycDocumentType documentType) {
        return springDataKycDocumentRepository.findByUserIdAndDocumentType(userId, documentType)
                .map(kycDocumentMapper::toDomain);
    }
}

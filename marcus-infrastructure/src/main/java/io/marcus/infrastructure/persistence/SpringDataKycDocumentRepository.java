package io.marcus.infrastructure.persistence;

import io.marcus.domain.vo.KycDocumentType;
import io.marcus.infrastructure.persistence.entity.KycDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataKycDocumentRepository extends JpaRepository<KycDocumentEntity, String> {
    List<KycDocumentEntity> findByUserId(String userId);
    Optional<KycDocumentEntity> findByUserIdAndDocumentType(String userId, KycDocumentType documentType);
}

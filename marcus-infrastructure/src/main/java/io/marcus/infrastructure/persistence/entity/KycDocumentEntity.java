package io.marcus.infrastructure.persistence.entity;

import io.marcus.domain.vo.KycDocumentType;
import io.marcus.domain.vo.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "kyc_documents", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "document_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class KycDocumentEntity extends BaseEntity {

    @Id
    @Column(name = "document_id", nullable = false, unique = true)
    private String documentId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private KycDocumentType documentType;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private KycStatus status;

    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;
}

package io.marcus.domain.model;

import io.marcus.domain.vo.KycDocumentType;
import io.marcus.domain.vo.KycStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class KycDocument extends BaseModel {
    private String documentId;
    private String userId;
    private KycDocumentType documentType;
    private String objectKey;
    private Long fileSize;
    private String contentType;
    private KycStatus status;
    private String rejectReason;
}

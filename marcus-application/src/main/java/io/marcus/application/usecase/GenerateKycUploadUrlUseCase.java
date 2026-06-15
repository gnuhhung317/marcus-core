package io.marcus.application.usecase;

import io.marcus.application.dto.KycUploadInitiateRequest;
import io.marcus.application.dto.KycUploadInitiateResponse;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.KycDocument;
import io.marcus.domain.port.StoragePort;
import io.marcus.domain.vo.PresignedPostData;
import io.marcus.domain.vo.KycDocumentType;
import io.marcus.domain.vo.KycStatus;
import io.marcus.domain.repository.KycDocumentRepository;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateKycUploadUrlUseCase {

    private final IdentityService identityService;
    private final KycDocumentRepository kycDocumentRepository;
    private final StoragePort storagePort;

    private static final long MIN_IMAGE_SIZE = 1024L; // 1 KB
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L; // 10 MB
    
    private static final long MIN_VIDEO_SIZE = 1024L; // 1 KB
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024L; // 50 MB

    @Transactional
    public KycUploadInitiateResponse execute(KycUploadInitiateRequest request) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (request.documentType() == null) {
            throw new IllegalArgumentException("Document type is required");
        }
        if (request.contentType() == null || request.contentType().isBlank()) {
            throw new IllegalArgumentException("Content type is required");
        }

        String contentType = request.contentType().trim().toLowerCase();
        validateContentType(request.documentType(), contentType);

        long minSize;
        long maxSize;
        if (request.documentType() == KycDocumentType.VIDEO) {
            minSize = MIN_VIDEO_SIZE;
            maxSize = MAX_VIDEO_SIZE;
        } else {
            minSize = MIN_IMAGE_SIZE;
            maxSize = MAX_IMAGE_SIZE;
        }

        // Check if user already has a document of this type
        KycDocument document = kycDocumentRepository.findByUserIdAndDocumentType(userId, request.documentType())
                .orElse(null);

        String documentId;
        String objectKey;
        if (document != null) {
            documentId = document.getDocumentId();
            // Re-use object key if it already has one, or generate new one. Generating a new key
            // is safer to avoid browser caching issues and handle clean overwrites.
            objectKey = String.format("kyc/%s/%s_%s", userId, request.documentType().name().toLowerCase(), UUID.randomUUID());
            
            document.setObjectKey(objectKey);
            document.setFileSize(null);
            document.setContentType(contentType);
            document.setStatus(KycStatus.PENDING_UPLOAD);
            document.setRejectReason(null);
            document.setUpdatedAt(LocalDateTime.now());
        } else {
            documentId = "kyc_" + UUID.randomUUID();
            objectKey = String.format("kyc/%s/%s_%s", userId, request.documentType().name().toLowerCase(), UUID.randomUUID());
            
            document = KycDocument.builder()
                    .documentId(documentId)
                    .userId(userId)
                    .documentType(request.documentType())
                    .objectKey(objectKey)
                    .status(KycStatus.PENDING_UPLOAD)
                    .contentType(contentType)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        PresignedPostData presignedPostData = storagePort.generatePresignedPostUploadUrl(
                objectKey, contentType, minSize, maxSize
        );

        kycDocumentRepository.save(document);
        log.info("Initiated KYC upload for user {}, docType: {}, docId: {}", userId, request.documentType(), documentId);

        return new KycUploadInitiateResponse(documentId, objectKey, presignedPostData);
    }

    private void validateContentType(KycDocumentType documentType, String contentType) {
        if (documentType == KycDocumentType.VIDEO) {
            if (!contentType.equals("video/mp4") && !contentType.equals("video/webm")) {
                throw new IllegalArgumentException("Only video/mp4 and video/webm content types are supported for verification video");
            }
        } else {
            if (!contentType.equals("image/jpeg") && !contentType.equals("image/jpg") && !contentType.equals("image/png")) {
                throw new IllegalArgumentException("Only image/jpeg and image/png content types are supported for ID documents");
            }
        }
    }
}

package io.marcus.application.usecase;

import io.marcus.application.dto.KycDocumentResponse;
import io.marcus.application.dto.KycUploadConfirmRequest;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.application.mapper.KycDocumentDtoMapper;
import io.marcus.domain.exception.KycDocumentNotFoundException;
import io.marcus.domain.model.KycDocument;
import io.marcus.domain.port.StoragePort;
import io.marcus.domain.vo.KycDocumentType;
import io.marcus.domain.vo.KycStatus;
import io.marcus.domain.vo.ObjectMetadata;
import io.marcus.domain.repository.KycDocumentRepository;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmKycUploadUseCase {

    private final IdentityService identityService;
    private final KycDocumentRepository kycDocumentRepository;
    private final StoragePort storagePort;
    private final VerifyKycDocumentMagicBytesService verifyKycDocumentMagicBytesService;
    private final KycDocumentDtoMapper kycDocumentDtoMapper;

    private static final long MIN_IMAGE_SIZE = 1024L; // 1 KB
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L; // 10 MB
    
    private static final long MIN_VIDEO_SIZE = 1024L; // 1 KB
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024L; // 50 MB

    @Transactional
    public KycDocumentResponse execute(KycUploadConfirmRequest request) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (request.documentId() == null || request.documentId().isBlank()) {
            throw new IllegalArgumentException("documentId is required");
        }

        KycDocument document = kycDocumentRepository.findById(request.documentId())
                .orElseThrow(() -> new KycDocumentNotFoundException("KYC Document not found with ID: " + request.documentId()));

        if (!userId.equals(document.getUserId())) {
            throw new ForbiddenOperationException("Document does not belong to the authenticated user");
        }

        if (document.getStatus() != KycStatus.PENDING_UPLOAD) {
            throw new IllegalStateException("Document upload has already been confirmed or processed. Current status: " + document.getStatus());
        }

        // Query MinIO metadata directly to verify physical presence and sizes
        ObjectMetadata metadata = storagePort.getObjectMetadata(document.getObjectKey());
        if (!metadata.isExists()) {
            log.warn("Physical file not found in storage for key: {}", document.getObjectKey());
            throw new IllegalStateException("File was not uploaded successfully to storage.");
        }

        long minSize = document.getDocumentType() == KycDocumentType.VIDEO ? MIN_VIDEO_SIZE : MIN_IMAGE_SIZE;
        long maxSize = document.getDocumentType() == KycDocumentType.VIDEO ? MAX_VIDEO_SIZE : MAX_IMAGE_SIZE;

        if (metadata.getSize() < minSize || metadata.getSize() > maxSize) {
            log.warn("Uploaded file size {} does not match boundaries [{} - {}]", metadata.getSize(), minSize, maxSize);
            document.setStatus(KycStatus.REJECTED);
            document.setRejectReason("File size violates constraints.");
            document.setUpdatedAt(LocalDateTime.now());
            kycDocumentRepository.save(document);
            return kycDocumentDtoMapper.toResponse(document);
        }

        // Update database with size and status from physical storage details
        document.setFileSize(metadata.getSize());
        document.setContentType(metadata.getContentType());
        document.setStatus(KycStatus.UPLOADED);
        document.setUpdatedAt(LocalDateTime.now());
        
        KycDocument saved = kycDocumentRepository.save(document);
        log.info("Document {} upload confirmed. Triggering asynchronous magic bytes verification...", saved.getDocumentId());

        // Asynchronously check file magic bytes
        verifyKycDocumentMagicBytesService.verifyMagicBytes(saved.getDocumentId());

        return kycDocumentDtoMapper.toResponse(saved);
    }
}

package io.marcus.application.usecase;

import io.marcus.domain.model.KycDocument;
import io.marcus.domain.port.StoragePort;
import io.marcus.domain.vo.KycDocumentType;
import io.marcus.domain.vo.KycStatus;
import io.marcus.domain.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyKycDocumentMagicBytesService {

    private final KycDocumentRepository kycDocumentRepository;
    private final StoragePort storagePort;

    /**
     * Asynchronously verifies the magic bytes of the uploaded KYC document
     * to prevent client-side extension spoofing.
     * Note: In a production S3/MinIO environment, this signature validation can
     * alternatively be triggered via MinIO bucket notification webhooks.
     */
    @Async
    @Transactional
    public void verifyMagicBytes(String documentId) {
        log.info("Starting async magic bytes verification for documentId: {}", documentId);
        
        KycDocument doc = kycDocumentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            log.warn("Document not found for verification: {}", documentId);
            return;
        }

        if (doc.getStatus() != KycStatus.UPLOADED) {
            log.warn("Document {} is not in UPLOADED status, skipping verification", documentId);
            return;
        }

        try {
            // Retrieve first 16 bytes for checking headers
            byte[] header = storagePort.fetchFirstNBytes(doc.getObjectKey(), 16);
            
            boolean isValid = validateSignature(header, doc.getContentType(), doc.getDocumentType());
            
            if (isValid) {
                doc.setStatus(KycStatus.APPROVED_FOR_REVIEW);
                log.info("Magic bytes verification PASSED for document {}. Updated status to APPROVED_FOR_REVIEW", documentId);
            } else {
                doc.setStatus(KycStatus.REJECTED);
                doc.setRejectReason("File signature (magic bytes) does not match the expected content type.");
                log.warn("Magic bytes verification FAILED for document {}. Mismatch detected for contentType: {}. Updated status to REJECTED", 
                        documentId, doc.getContentType());
            }
        } catch (Exception e) {
            log.error("Failed to perform magic bytes verification for document: {}", documentId, e);
            doc.setStatus(KycStatus.REJECTED);
            doc.setRejectReason("Failed to verify file integrity check.");
        }

        kycDocumentRepository.save(doc);
    }

    private boolean validateSignature(byte[] bytes, String contentType, KycDocumentType documentType) {
        if (bytes == null || bytes.length < 4) {
            log.warn("File header is too small to verify magic bytes: {} bytes", bytes != null ? bytes.length : 0);
            return false;
        }

        // JPEG/JPG: Starts with FF D8 FF
        if (contentType.equals("image/jpeg") || contentType.equals("image/jpg")) {
            return (bytes[0] & 0xFF) == 0xFF && 
                   (bytes[1] & 0xFF) == 0xD8 && 
                   (bytes[2] & 0xFF) == 0xFF;
        }

        // PNG: Starts with 89 50 4E 47 (Hex)
        if (contentType.equals("image/png")) {
            return (bytes[0] & 0xFF) == 0x89 && 
                   (bytes[1] & 0xFF) == 0x50 && 
                   (bytes[2] & 0xFF) == 0x4E && 
                   (bytes[3] & 0xFF) == 0x47;
        }

        // WebM: Starts with 1A 45 DF A3 (Hex)
        if (contentType.equals("video/webm")) {
            return (bytes[0] & 0xFF) == 0x1A && 
                   (bytes[1] & 0xFF) == 0x45 && 
                   (bytes[2] & 0xFF) == 0xDF && 
                   (bytes[3] & 0xFF) == 0xA3;
        }

        // MP4: Starts with 'ftyp' at offset 4 (Hex 66 74 79 70)
        if (contentType.equals("video/mp4")) {
            if (bytes.length < 8) return false;
            return (bytes[4] & 0xFF) == 0x66 && 
                   (bytes[5] & 0xFF) == 0x74 && 
                   (bytes[6] & 0xFF) == 0x79 && 
                   (bytes[7] & 0xFF) == 0x70;
        }

        log.warn("Unsupported content type for magic bytes verification: {}", contentType);
        return false;
    }
}

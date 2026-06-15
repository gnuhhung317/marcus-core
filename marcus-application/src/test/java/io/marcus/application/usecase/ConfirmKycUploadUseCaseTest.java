package io.marcus.application.usecase;

import io.marcus.application.dto.KycDocumentResponse;
import io.marcus.application.dto.KycUploadConfirmRequest;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.application.mapper.KycDocumentDtoMapper;
import io.marcus.domain.model.KycDocument;
import io.marcus.domain.port.StoragePort;
import io.marcus.domain.repository.KycDocumentRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.KycDocumentType;
import io.marcus.domain.vo.KycStatus;
import io.marcus.domain.vo.ObjectMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmKycUploadUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private KycDocumentRepository kycDocumentRepository;

    @Mock
    private StoragePort storagePort;

    @Mock
    private VerifyKycDocumentMagicBytesService verifyKycDocumentMagicBytesService;

    @Spy
    private KycDocumentDtoMapper kycDocumentDtoMapper = new KycDocumentDtoMapper();

    @InjectMocks
    private ConfirmKycUploadUseCase confirmKycUploadUseCase;

    @Test
    void shouldConfirmUploadSuccessfully() {
        // Given
        String userId = "user-123";
        String docId = "doc-456";
        KycUploadConfirmRequest request = new KycUploadConfirmRequest(docId);
        
        KycDocument pendingDoc = KycDocument.builder()
                .documentId(docId)
                .userId(userId)
                .documentType(KycDocumentType.ID_CARD_FRONT)
                .objectKey("kyc/user-123/id_front")
                .status(KycStatus.PENDING_UPLOAD)
                .build();

        ObjectMetadata metadata = ObjectMetadata.builder()
                .exists(true)
                .size(500 * 1024L)
                .contentType("image/png")
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(kycDocumentRepository.findById(docId)).thenReturn(Optional.of(pendingDoc));
        when(storagePort.getObjectMetadata(pendingDoc.getObjectKey())).thenReturn(metadata);
        when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        KycDocumentResponse response = confirmKycUploadUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertEquals(KycStatus.UPLOADED, response.status());
        assertEquals(500 * 1024L, response.fileSize());
        assertEquals("image/png", response.contentType());
        
        verify(verifyKycDocumentMagicBytesService).verifyMagicBytes(docId);
    }

    @Test
    void shouldRejectWhenUploadedFileSizeViolatesConstraints() {
        // Given
        String userId = "user-123";
        String docId = "doc-456";
        KycUploadConfirmRequest request = new KycUploadConfirmRequest(docId);
        
        KycDocument pendingDoc = KycDocument.builder()
                .documentId(docId)
                .userId(userId)
                .documentType(KycDocumentType.ID_CARD_FRONT)
                .objectKey("kyc/user-123/id_front")
                .status(KycStatus.PENDING_UPLOAD)
                .build();

        // Size is 50 bytes (which is < 1KB boundary)
        ObjectMetadata metadata = ObjectMetadata.builder()
                .exists(true)
                .size(50L)
                .contentType("image/png")
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(kycDocumentRepository.findById(docId)).thenReturn(Optional.of(pendingDoc));
        when(storagePort.getObjectMetadata(pendingDoc.getObjectKey())).thenReturn(metadata);
        when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        KycDocumentResponse response = confirmKycUploadUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertEquals(KycStatus.REJECTED, response.status());
        assertEquals("File size violates constraints.", response.rejectionReason());
        
        verifyNoInteractions(verifyKycDocumentMagicBytesService);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotOwnDocument() {
        // Given
        String userId = "user-123";
        String docId = "doc-456";
        KycUploadConfirmRequest request = new KycUploadConfirmRequest(docId);
        
        KycDocument pendingDoc = KycDocument.builder()
                .documentId(docId)
                .userId("different-user")
                .documentType(KycDocumentType.ID_CARD_FRONT)
                .objectKey("kyc/diff/id_front")
                .status(KycStatus.PENDING_UPLOAD)
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(kycDocumentRepository.findById(docId)).thenReturn(Optional.of(pendingDoc));

        // When/Then
        assertThrows(ForbiddenOperationException.class, () -> confirmKycUploadUseCase.execute(request));
        verifyNoInteractions(storagePort, verifyKycDocumentMagicBytesService);
    }
}

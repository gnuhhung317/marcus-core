package io.marcus.application.usecase;

import io.marcus.application.dto.KycUploadInitiateRequest;
import io.marcus.application.dto.KycUploadInitiateResponse;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.KycDocument;
import io.marcus.domain.port.StoragePort;
import io.marcus.domain.repository.KycDocumentRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.KycDocumentType;
import io.marcus.domain.vo.KycStatus;
import io.marcus.domain.vo.PresignedPostData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateKycUploadUrlUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private KycDocumentRepository kycDocumentRepository;

    @Mock
    private StoragePort storagePort;

    @InjectMocks
    private GenerateKycUploadUrlUseCase generateKycUploadUrlUseCase;

    @Test
    void shouldInitiateUploadForValidImageRequest() {
        // Given
        String userId = "user-123";
        KycUploadInitiateRequest request = new KycUploadInitiateRequest(KycDocumentType.ID_CARD_FRONT, "image/png");
        PresignedPostData presignedPostData = PresignedPostData.builder()
                .uploadUrl("http://minio/bucket")
                .formData(Map.of("key", "val"))
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(kycDocumentRepository.findByUserIdAndDocumentType(userId, KycDocumentType.ID_CARD_FRONT))
                .thenReturn(Optional.empty());
        when(storagePort.generatePresignedPostUploadUrl(anyString(), eq("image/png"), eq(1024L), eq(10 * 1024 * 1024L)))
                .thenReturn(presignedPostData);

        // When
        KycUploadInitiateResponse response = generateKycUploadUrlUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertEquals(presignedPostData, response.presignedPostData());
        assertTrue(response.objectKey().startsWith("kyc/user-123/id_card_front_"));

        ArgumentCaptor<KycDocument> docCaptor = ArgumentCaptor.forClass(KycDocument.class);
        verify(kycDocumentRepository).save(docCaptor.capture());
        
        KycDocument savedDoc = docCaptor.getValue();
        assertEquals(userId, savedDoc.getUserId());
        assertEquals(KycDocumentType.ID_CARD_FRONT, savedDoc.getDocumentType());
        assertEquals(KycStatus.PENDING_UPLOAD, savedDoc.getStatus());
        assertEquals("image/png", savedDoc.getContentType());
        assertNotNull(savedDoc.getDocumentId());
    }

    @Test
    void shouldInitiateUploadForValidVideoRequest() {
        // Given
        String userId = "user-123";
        KycUploadInitiateRequest request = new KycUploadInitiateRequest(KycDocumentType.VIDEO, "video/mp4");
        PresignedPostData presignedPostData = PresignedPostData.builder()
                .uploadUrl("http://minio/bucket")
                .formData(Map.of("key", "val"))
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(kycDocumentRepository.findByUserIdAndDocumentType(userId, KycDocumentType.VIDEO))
                .thenReturn(Optional.empty());
        when(storagePort.generatePresignedPostUploadUrl(anyString(), eq("video/mp4"), eq(1024L), eq(50 * 1024 * 1024L)))
                .thenReturn(presignedPostData);

        // When
        KycUploadInitiateResponse response = generateKycUploadUrlUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertEquals(presignedPostData, response.presignedPostData());
        assertTrue(response.objectKey().startsWith("kyc/user-123/video_"));

        ArgumentCaptor<KycDocument> docCaptor = ArgumentCaptor.forClass(KycDocument.class);
        verify(kycDocumentRepository).save(docCaptor.capture());
        
        KycDocument savedDoc = docCaptor.getValue();
        assertEquals(userId, savedDoc.getUserId());
        assertEquals(KycDocumentType.VIDEO, savedDoc.getDocumentType());
        assertEquals("video/mp4", savedDoc.getContentType());
    }

    @Test
    void shouldThrowExceptionWhenUnauthenticated() {
        KycUploadInitiateRequest request = new KycUploadInitiateRequest(KycDocumentType.ID_CARD_FRONT, "image/png");
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(UnauthenticatedException.class, () -> generateKycUploadUrlUseCase.execute(request));
        verifyNoInteractions(storagePort, kycDocumentRepository);
    }

    @Test
    void shouldThrowExceptionWhenInvalidContentType() {
        String userId = "user-123";
        KycUploadInitiateRequest request = new KycUploadInitiateRequest(KycDocumentType.ID_CARD_FRONT, "application/pdf");
        when(identityService.getCurrentUserId()).thenReturn(Optional.of(userId));

        assertThrows(IllegalArgumentException.class, () -> generateKycUploadUrlUseCase.execute(request));
        verifyNoInteractions(storagePort, kycDocumentRepository);
    }
}

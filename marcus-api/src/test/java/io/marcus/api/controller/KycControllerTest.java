package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.dto.KycDocumentResponse;
import io.marcus.application.dto.KycUploadConfirmRequest;
import io.marcus.application.dto.KycUploadInitiateRequest;
import io.marcus.application.dto.KycUploadInitiateResponse;
import io.marcus.application.usecase.ConfirmKycUploadUseCase;
import io.marcus.application.usecase.GenerateKycUploadUrlUseCase;
import io.marcus.domain.vo.KycDocumentType;
import io.marcus.domain.vo.KycStatus;
import io.marcus.domain.vo.PresignedPostData;
import io.marcus.infrastructure.security.BotSignatureInterceptor;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KycController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class KycControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GenerateKycUploadUrlUseCase generateKycUploadUrlUseCase;

    @MockBean
    private ConfirmKycUploadUseCase confirmKycUploadUseCase;

    @MockBean
    private io.marcus.application.usecase.ListKycDocumentsUseCase listKycDocumentsUseCase;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @BeforeEach
    void setUpFilters() throws Exception {
        doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> true)
                .when(botSignatureInterceptor)
                .preHandle(any(), any(), any());
    }

    @Test
    void shouldInitiateUploadUrl() throws Exception {
        KycUploadInitiateRequest request = new KycUploadInitiateRequest(KycDocumentType.ID_CARD_FRONT, "image/png");
        PresignedPostData postData = PresignedPostData.builder()
                .uploadUrl("http://minio/bucket")
                .formData(Map.of("key", "kyc/user/id_front"))
                .build();
        KycUploadInitiateResponse response = new KycUploadInitiateResponse(
                "doc-123",
                "kyc/user/id_front",
                postData
        );

        when(generateKycUploadUrlUseCase.execute(any(KycUploadInitiateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/kyc/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("doc-123"))
                .andExpect(jsonPath("$.objectKey").value("kyc/user/id_front"))
                .andExpect(jsonPath("$.presignedPostData.uploadUrl").value("http://minio/bucket"))
                .andExpect(jsonPath("$.presignedPostData.formData.key").value("kyc/user/id_front"));
    }

    @Test
    void shouldConfirmUpload() throws Exception {
        KycUploadConfirmRequest request = new KycUploadConfirmRequest("doc-123");
        KycDocumentResponse response = new KycDocumentResponse(
                "doc-123",
                "user-123",
                KycDocumentType.ID_CARD_FRONT,
                "kyc/user/id_front",
                KycStatus.UPLOADED,
                100 * 1024L,
                "image/png",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(confirmKycUploadUseCase.execute(any(KycUploadConfirmRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/kyc/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("doc-123"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.fileSize").value(100 * 1024));
    }
}

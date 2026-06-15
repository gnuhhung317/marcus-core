package io.marcus.api.controller;

import io.marcus.application.dto.KycDocumentResponse;
import io.marcus.application.dto.KycUploadConfirmRequest;
import io.marcus.application.dto.KycUploadInitiateRequest;
import io.marcus.application.dto.KycUploadInitiateResponse;
import io.marcus.application.usecase.ConfirmKycUploadUseCase;
import io.marcus.application.usecase.GenerateKycUploadUrlUseCase;
import io.marcus.application.usecase.ListKycDocumentsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/kyc", "/api/kyc", "/api/v1/kyc"})
@RequiredArgsConstructor
public class KycController {

    private final GenerateKycUploadUrlUseCase generateKycUploadUrlUseCase;
    private final ConfirmKycUploadUseCase confirmKycUploadUseCase;
    private final ListKycDocumentsUseCase listKycDocumentsUseCase;

    @PostMapping("/upload-url")
    public ResponseEntity<KycUploadInitiateResponse> initiateUpload(
            @Valid @RequestBody KycUploadInitiateRequest request
    ) {
        return ResponseEntity.ok(generateKycUploadUrlUseCase.execute(request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<KycDocumentResponse> confirmUpload(
            @Valid @RequestBody KycUploadConfirmRequest request
    ) {
        return ResponseEntity.ok(confirmKycUploadUseCase.execute(request));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<KycDocumentResponse>> getMyDocuments() {
        return ResponseEntity.ok(listKycDocumentsUseCase.execute());
    }
}

package io.marcus.application.usecase;

import io.marcus.application.dto.KycDocumentResponse;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.application.mapper.KycDocumentDtoMapper;
import io.marcus.domain.repository.KycDocumentRepository;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListKycDocumentsUseCase {

    private final IdentityService identityService;
    private final KycDocumentRepository kycDocumentRepository;
    private final KycDocumentDtoMapper kycDocumentDtoMapper;

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> execute() {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        log.debug("Listing KYC documents for user: {}", userId);
        return kycDocumentRepository.findByUserId(userId).stream()
                .map(kycDocumentDtoMapper::toResponse)
                .toList();
    }
}

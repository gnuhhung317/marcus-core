package io.marcus.api.controller;

import io.marcus.application.usecase.GetCurrentUserProfileUseCase;
import io.marcus.application.usecase.ListCurrentUserApiKeysUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/users", "/api/users", "/api/v1/users"})
@RequiredArgsConstructor
public class UserProfileController {

    private final GetCurrentUserProfileUseCase getCurrentUserProfileUseCase;
    private final ListCurrentUserApiKeysUseCase listCurrentUserApiKeysUseCase;

    @GetMapping("/me")
    public ResponseEntity<TerminalReadPort.UserProfileSnapshot> getCurrentUserProfile() {
        return ResponseEntity.ok(getCurrentUserProfileUseCase.execute());
    }

    @GetMapping("/me/api-keys")
    public ResponseEntity<List<TerminalReadPort.ApiKeySummarySnapshot>> listCurrentUserApiKeys() {
        return ResponseEntity.ok(listCurrentUserApiKeysUseCase.execute());
    }
}

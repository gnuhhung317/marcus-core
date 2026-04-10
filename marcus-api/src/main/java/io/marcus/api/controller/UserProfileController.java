package io.marcus.api.controller;

import io.marcus.application.dto.CreateApiKeyRequest;
import io.marcus.application.dto.UpdateUserPreferencesRequest;
import io.marcus.application.usecase.CreateCurrentUserApiKeyUseCase;
import io.marcus.application.usecase.DeleteCurrentUserApiKeyUseCase;
import io.marcus.application.usecase.GetCurrentUserProfileUseCase;
import io.marcus.application.usecase.ListCurrentUserApiKeysUseCase;
import io.marcus.application.usecase.ListCurrentUserLoginActivitiesUseCase;
import io.marcus.application.usecase.UpdateCurrentUserPreferencesUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/users", "/api/users", "/api/v1/users"})
@RequiredArgsConstructor
public class UserProfileController {

    private final GetCurrentUserProfileUseCase getCurrentUserProfileUseCase;
    private final ListCurrentUserApiKeysUseCase listCurrentUserApiKeysUseCase;
    private final UpdateCurrentUserPreferencesUseCase updateCurrentUserPreferencesUseCase;
    private final CreateCurrentUserApiKeyUseCase createCurrentUserApiKeyUseCase;
    private final DeleteCurrentUserApiKeyUseCase deleteCurrentUserApiKeyUseCase;
    private final ListCurrentUserLoginActivitiesUseCase listCurrentUserLoginActivitiesUseCase;

    @GetMapping("/me")
    public ResponseEntity<TerminalReadPort.UserProfileSnapshot> getCurrentUserProfile() {
        return ResponseEntity.ok(getCurrentUserProfileUseCase.execute());
    }

    @GetMapping("/me/api-keys")
    public ResponseEntity<List<TerminalReadPort.ApiKeySummarySnapshot>> listCurrentUserApiKeys() {
        return ResponseEntity.ok(listCurrentUserApiKeysUseCase.execute());
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<TerminalReadPort.UserPreferencesSnapshot> updateCurrentUserPreferences(
            @RequestBody UpdateUserPreferencesRequest request
    ) {
        return ResponseEntity.ok(updateCurrentUserPreferencesUseCase.execute(request));
    }

    @PostMapping("/me/api-keys")
    public ResponseEntity<TerminalReadPort.CreateApiKeySnapshot> createCurrentUserApiKey(
            @RequestBody CreateApiKeyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createCurrentUserApiKeyUseCase.execute(request));
    }

    @DeleteMapping("/me/api-keys/{apiKeyId}")
    public ResponseEntity<Void> deleteCurrentUserApiKey(@PathVariable String apiKeyId) {
        deleteCurrentUserApiKeyUseCase.execute(apiKeyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/login-activities")
    public ResponseEntity<TerminalReadPort.LoginActivityPageSnapshot> listCurrentUserLoginActivities(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(listCurrentUserLoginActivitiesUseCase.execute(page, size));
    }
}

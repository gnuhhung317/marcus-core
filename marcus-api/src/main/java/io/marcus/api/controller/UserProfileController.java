package io.marcus.api.controller;

import io.marcus.application.dto.UpdateUserProfileRequest;
import io.marcus.application.usecase.GetCurrentUserProfileUseCase;
import io.marcus.application.usecase.UpdateCurrentUserProfileUseCase;
import io.marcus.domain.port.UserProfileReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/users", "/api/users", "/api/v1/users"})
@RequiredArgsConstructor
public class UserProfileController {

    private final GetCurrentUserProfileUseCase getCurrentUserProfileUseCase;
    private final UpdateCurrentUserProfileUseCase updateCurrentUserProfileUseCase;

    @GetMapping("/me")
    public ResponseEntity<UserProfileReadPort.UserProfileSnapshot> getCurrentUserProfile() {
        return ResponseEntity.ok(getCurrentUserProfileUseCase.execute());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileReadPort.UserProfileSnapshot> updateCurrentUserProfile(
            @RequestBody UpdateUserProfileRequest request
    ) {
        return ResponseEntity.ok(updateCurrentUserProfileUseCase.execute(request));
    }
}

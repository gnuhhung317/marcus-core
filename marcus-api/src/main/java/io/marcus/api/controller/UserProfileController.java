package io.marcus.api.controller;

import io.marcus.application.dto.UpdateUserProfileRequest;
import io.marcus.application.usecase.GetCurrentUserProfileUseCase;
import io.marcus.application.usecase.ListCurrentUserLoginActivitiesUseCase;
import io.marcus.application.usecase.UpdateCurrentUserProfileUseCase;
import io.marcus.domain.port.UserProfileReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/users", "/api/users", "/api/v1/users"})
@RequiredArgsConstructor
public class UserProfileController {

    private final GetCurrentUserProfileUseCase getCurrentUserProfileUseCase;
    private final UpdateCurrentUserProfileUseCase updateCurrentUserProfileUseCase;
    private final ListCurrentUserLoginActivitiesUseCase listCurrentUserLoginActivitiesUseCase;

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

    @GetMapping("/me/login-activities")
    public ResponseEntity<UserProfileReadPort.LoginActivityPageSnapshot> listCurrentUserLoginActivities(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(listCurrentUserLoginActivitiesUseCase.execute(page, size));
    }
}

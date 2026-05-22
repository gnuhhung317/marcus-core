package io.marcus.application.usecase;

import io.marcus.application.dto.UpdateUserProfileRequest;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.UserProfileReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateCurrentUserProfileUseCase {

    private final IdentityService identityService;
    private final UserProfileReadPort userProfileReadPort;

    public UserProfileReadPort.UserProfileSnapshot execute(UpdateUserProfileRequest request) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        return userProfileReadPort.updateCurrentUserProfile(
                userId,
                new UserProfileReadPort.UserProfileUpdateSnapshot(
                        request.username(),
                        request.email()
                )
        );
    }
}

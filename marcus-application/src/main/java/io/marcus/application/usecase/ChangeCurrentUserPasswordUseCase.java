package io.marcus.application.usecase;

import io.marcus.application.dto.ChangeCurrentUserPasswordRequest;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.UserProfileReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangeCurrentUserPasswordUseCase {

    private final IdentityService identityService;
    private final UserProfileReadPort userProfileReadPort;

    public UserProfileReadPort.UserProfileSnapshot execute(ChangeCurrentUserPasswordRequest request) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        return userProfileReadPort.changeCurrentUserPassword(
                userId,
                new UserProfileReadPort.UserPasswordUpdateSnapshot(
                        request.currentPassword(),
                        request.newPassword()
                )
        );
    }
}

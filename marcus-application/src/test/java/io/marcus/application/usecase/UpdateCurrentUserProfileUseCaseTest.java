package io.marcus.application.usecase;

import io.marcus.application.dto.UpdateUserProfileRequest;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.UserProfileReadPort;
import io.marcus.domain.service.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCurrentUserProfileUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private UserProfileReadPort userProfileReadPort;

    private UpdateCurrentUserProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateCurrentUserProfileUseCase(identityService, userProfileReadPort);
    }

    @Test
    void shouldUpdateProfileForCurrentUser() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("trader_2", "trader2@marcus.local");
        UserProfileReadPort.UserProfileSnapshot response = new UserProfileReadPort.UserProfileSnapshot("usr_1", "trader_2", "trader2@marcus.local", "USER");

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userProfileReadPort.updateCurrentUserProfile(
                "usr_1",
                new UserProfileReadPort.UserProfileUpdateSnapshot("trader_2", "trader2@marcus.local")
        )).thenReturn(response);

        UserProfileReadPort.UserProfileSnapshot result = useCase.execute(request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateUserProfileRequest("trader_2", "trader2@marcus.local")))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }
}

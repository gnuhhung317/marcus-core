package io.marcus.application.usecase;

import io.marcus.application.dto.ChangeCurrentUserPasswordRequest;
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
class ChangeCurrentUserPasswordUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private UserProfileReadPort userProfileReadPort;

    private ChangeCurrentUserPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangeCurrentUserPasswordUseCase(identityService, userProfileReadPort);
    }

    @Test
    void shouldChangePasswordForCurrentUser() {
        ChangeCurrentUserPasswordRequest request = new ChangeCurrentUserPasswordRequest("current-pass", "new-pass-123");
        UserProfileReadPort.UserProfileSnapshot response = new UserProfileReadPort.UserProfileSnapshot("usr_1", "trader_1", "trader@marcus.local", "TRADER");

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userProfileReadPort.changeCurrentUserPassword(
                "usr_1",
                new UserProfileReadPort.UserPasswordUpdateSnapshot("current-pass", "new-pass-123")
        )).thenReturn(response);

        UserProfileReadPort.UserProfileSnapshot result = useCase.execute(request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ChangeCurrentUserPasswordRequest("current-pass", "new-pass-123")))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }
}

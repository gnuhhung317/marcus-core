package io.marcus.application.usecase;

import io.marcus.application.dto.UpdateUserPreferencesRequest;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
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
class UpdateCurrentUserPreferencesUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private TerminalReadPort terminalReadPort;

    private UpdateCurrentUserPreferencesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateCurrentUserPreferencesUseCase(identityService, terminalReadPort);
    }

    @Test
    void shouldUpdatePreferencesForCurrentUser() {
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest("UTC", "en-US", true);
        TerminalReadPort.UserPreferencesSnapshot response = new TerminalReadPort.UserPreferencesSnapshot("UTC", "en-US", true);

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(terminalReadPort.updateCurrentUserPreferences(
                "usr_1",
                new TerminalReadPort.UserPreferencesUpdateSnapshot("UTC", "en-US", true)
        )).thenReturn(response);

        TerminalReadPort.UserPreferencesSnapshot result = useCase.execute(request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateUserPreferencesRequest("UTC", "en-US", true)))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }
}

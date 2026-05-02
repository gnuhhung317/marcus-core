package io.marcus.application.usecase;

import io.marcus.application.dto.CreateApiKeyRequest;
import io.marcus.application.exception.ResourceConflictException;
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
class CreateCurrentUserApiKeyUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private TerminalReadPort terminalReadPort;

    private CreateCurrentUserApiKeyUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateCurrentUserApiKeyUseCase(identityService, terminalReadPort);
    }

    @Test
    void shouldCreateApiKeyForCurrentUser() {
        TerminalReadPort.CreateApiKeySnapshot response = new TerminalReadPort.CreateApiKeySnapshot("key_1", "mk_live_secret", "Terminal");

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(terminalReadPort.createCurrentUserApiKey("usr_1", "Terminal")).thenReturn(response);

        TerminalReadPort.CreateApiKeySnapshot result = useCase.execute(new CreateApiKeyRequest("Terminal"));

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new CreateApiKeyRequest("Terminal")))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }

    @Test
    void shouldThrowWhenLabelIsBlank() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));

        assertThatThrownBy(() -> useCase.execute(new CreateApiKeyRequest("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API key label is required");
    }

    @Test
    void shouldThrowWhenLabelConflicts() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));

        assertThatThrownBy(() -> useCase.execute(new CreateApiKeyRequest("default")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("API key label already exists");
    }
}

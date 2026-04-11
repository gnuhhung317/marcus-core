package io.marcus.application.usecase;

import io.marcus.application.dto.RemoveUserSessionRequest;
import io.marcus.domain.port.UserSessionRoutingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RemoveUserSessionUseCaseTest {

    @Mock
    private UserSessionRoutingPort userSessionRoutingPort;

    private RemoveUserSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RemoveUserSessionUseCase(userSessionRoutingPort);
    }

    @Test
    void shouldRemoveSessionWhenRequestIsValid() {
        RemoveUserSessionRequest request = new RemoveUserSessionRequest(" user-1 ", " session-1 ");

        useCase.execute(request);

        verify(userSessionRoutingPort).removeSession("user-1", "session-1");
    }

    @Test
    void shouldThrowWhenRequestIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Remove user session request is required");

        verifyNoInteractions(userSessionRoutingPort);
    }

    @Test
    void shouldThrowWhenUserIdIsMissing() {
        RemoveUserSessionRequest request = new RemoveUserSessionRequest(" ", "session-1");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User id is required");

        verifyNoInteractions(userSessionRoutingPort);
    }

    @Test
    void shouldThrowWhenSessionIdIsMissing() {
        RemoveUserSessionRequest request = new RemoveUserSessionRequest("user-1", " ");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Session id is required");

        verifyNoInteractions(userSessionRoutingPort);
    }
}
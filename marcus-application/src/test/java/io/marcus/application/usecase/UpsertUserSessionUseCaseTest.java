package io.marcus.application.usecase;

import io.marcus.application.dto.UpsertUserSessionRequest;
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
class UpsertUserSessionUseCaseTest {

    @Mock
    private UserSessionRoutingPort userSessionRoutingPort;

    private UpsertUserSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpsertUserSessionUseCase(userSessionRoutingPort);
    }

    @Test
    void shouldUpsertSessionWhenRequestIsValid() {
        UpsertUserSessionRequest request = new UpsertUserSessionRequest(" user-1 ", " session-1 ", " ws-1 ");

        useCase.execute(request);

        verify(userSessionRoutingPort).upsertSession("user-1", "session-1", "ws-1");
    }

    @Test
    void shouldThrowWhenRequestIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Upsert user session request is required");

        verifyNoInteractions(userSessionRoutingPort);
    }

    @Test
    void shouldThrowWhenUserIdIsMissing() {
        UpsertUserSessionRequest request = new UpsertUserSessionRequest(" ", "session-1", "ws-1");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User id is required");

        verifyNoInteractions(userSessionRoutingPort);
    }

    @Test
    void shouldThrowWhenSessionIdIsMissing() {
        UpsertUserSessionRequest request = new UpsertUserSessionRequest("user-1", " ", "ws-1");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Session id is required");

        verifyNoInteractions(userSessionRoutingPort);
    }

    @Test
    void shouldThrowWhenServerIdIsMissing() {
        UpsertUserSessionRequest request = new UpsertUserSessionRequest("user-1", "session-1", " ");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Server id is required");

        verifyNoInteractions(userSessionRoutingPort);
    }
}

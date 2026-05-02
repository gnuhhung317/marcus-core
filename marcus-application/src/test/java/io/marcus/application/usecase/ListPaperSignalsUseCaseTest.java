package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPaperSignalsUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private TerminalReadPort terminalReadPort;

    private ListPaperSignalsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListPaperSignalsUseCase(identityService, terminalReadPort);
    }

    @Test
    void shouldRequireAuthenticatedUserAndNormalizeInputs() {
        List<TerminalReadPort.PaperSignalSnapshot> expected = List.of(
                new TerminalReadPort.PaperSignalSnapshot("sig_1", "bot_1", "BTCUSDT", "BUY", 0.7, "ACTIVE", LocalDateTime.now())
        );
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(terminalReadPort.listPaperSignals("ACTIVE", 200)).thenReturn(expected);

        List<TerminalReadPort.PaperSignalSnapshot> result = useCase.execute(" active ", 999);

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void shouldThrowWhenStatusUnsupported() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));

        assertThatThrownBy(() -> useCase.execute("NEW", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported status: NEW");
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("ALL", 20))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }
}

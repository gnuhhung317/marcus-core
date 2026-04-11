package io.marcus.application.usecase;

import io.marcus.application.exception.ResourceConflictException;
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
class PausePaperSessionUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private TerminalReadPort terminalReadPort;

    private PausePaperSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PausePaperSessionUseCase(identityService, terminalReadPort);
    }

    @Test
    void shouldPauseSessionWhenRunning() {
        TerminalReadPort.PaperSessionStateSnapshot expected = new TerminalReadPort.PaperSessionStateSnapshot("ps_1", "PAUSED");
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(terminalReadPort.getPaperSessionSummary("usr_1"))
                .thenReturn(new TerminalReadPort.PaperSessionSummarySnapshot("ps_1", "RUNNING", 10000, 20, 4000));
        when(terminalReadPort.pausePaperSession("usr_1")).thenReturn(expected);

        TerminalReadPort.PaperSessionStateSnapshot result = useCase.execute();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowConflictWhenAlreadyPaused() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(terminalReadPort.getPaperSessionSummary("usr_1"))
                .thenReturn(new TerminalReadPort.PaperSessionSummarySnapshot("ps_1", "PAUSED", 10000, 20, 4000));

        assertThatThrownBy(() -> useCase.execute())
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Paper session is already paused");
    }
}

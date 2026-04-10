package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPaperExecutionLogsUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private TerminalReadPort terminalReadPort;

    private ListPaperExecutionLogsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListPaperExecutionLogsUseCase(identityService, terminalReadPort);
    }

    @Test
    void shouldNormalizeLimitAndCursor() {
        TerminalReadPort.PaperExecutionLogPageSnapshot page = new TerminalReadPort.PaperExecutionLogPageSnapshot(
                List.of(),
                new TerminalReadPort.CursorPaginationMetaSnapshot(null, "paper-cursor-200", 200, true)
        );

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(terminalReadPort.listPaperExecutionLogs("usr_1", "paper-cursor-10", 200)).thenReturn(page);

        TerminalReadPort.PaperExecutionLogPageSnapshot result = useCase.execute(" paper-cursor-10 ", 300);

        assertThat(result).isEqualTo(page);
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(null, 20))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }
}

package io.marcus.application.usecase;

import io.marcus.domain.exception.ResourceConflictException;
import io.marcus.domain.port.PortfolioReadPort;
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
class ResumePaperSessionUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private PortfolioReadPort portfolioReadPort;

    private ResumePaperSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResumePaperSessionUseCase(identityService, portfolioReadPort);
    }

    @Test
    void shouldResumeSessionWhenPaused() {
        PortfolioReadPort.PaperSessionStateSnapshot expected = new PortfolioReadPort.PaperSessionStateSnapshot("ps_1", "RUNNING");
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(portfolioReadPort.getPaperSessionSummary("usr_1"))
                .thenReturn(new PortfolioReadPort.PaperSessionSummarySnapshot("ps_1", "PAUSED", 10000, 20, 4000));
        when(portfolioReadPort.resumePaperSession("usr_1")).thenReturn(expected);

        PortfolioReadPort.PaperSessionStateSnapshot result = useCase.execute();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowConflictWhenAlreadyRunning() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(portfolioReadPort.getPaperSessionSummary("usr_1"))
                .thenReturn(new PortfolioReadPort.PaperSessionSummarySnapshot("ps_1", "RUNNING", 10000, 20, 4000));

        assertThatThrownBy(() -> useCase.execute())
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Paper session is already running");
    }
}

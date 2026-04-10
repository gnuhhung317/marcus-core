package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStrategyMetricsUseCaseTest {

    @Mock
    private TerminalReadPort terminalReadPort;

    private GetStrategyMetricsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStrategyMetricsUseCase(terminalReadPort);
    }

    @Test
    void shouldReturnMetricsWithNormalizedFeeMode() {
        TerminalReadPort.StrategyMetricsSnapshot expected = new TerminalReadPort.StrategyMetricsSnapshot(
                0.2, 0.1, 1.2, 1.4, 1.1, 1.9
        );
        when(terminalReadPort.getStrategyMetrics("stg_1", "AFTER_FEES")).thenReturn(expected);

        TerminalReadPort.StrategyMetricsSnapshot result = useCase.execute(" stg_1 ", " after_fees ");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenFeeModeUnsupported() {
        assertThatThrownBy(() -> useCase.execute("stg_1", "NET"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported fee mode: NET");
    }
}

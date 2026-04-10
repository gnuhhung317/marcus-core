package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStrategyPerformanceSeriesUseCaseTest {

    @Mock
    private TerminalReadPort terminalReadPort;

    private GetStrategyPerformanceSeriesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStrategyPerformanceSeriesUseCase(terminalReadPort);
    }

    @Test
    void shouldReturnSeriesWithNormalizedRange() {
        List<TerminalReadPort.TimeSeriesPointSnapshot> expected = List.of(
                new TerminalReadPort.TimeSeriesPointSnapshot(LocalDateTime.of(2026, 4, 1, 10, 0), 100.0)
        );
        when(terminalReadPort.listStrategyPerformanceSeries("stg_1", "1M")).thenReturn(expected);

        List<TerminalReadPort.TimeSeriesPointSnapshot> result = useCase.execute("stg_1", "1m");

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void shouldThrowWhenRangeUnsupported() {
        assertThatThrownBy(() -> useCase.execute("stg_1", "3M"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported range: 3M");
    }
}

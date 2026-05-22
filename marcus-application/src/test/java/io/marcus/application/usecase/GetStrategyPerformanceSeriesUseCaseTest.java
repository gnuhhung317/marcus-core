package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.port.PortfolioReadPort.TimeSeriesPointSnapshot;
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
    private BotDiscoveryReadPort botDiscoveryReadPort;

    private GetStrategyPerformanceSeriesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStrategyPerformanceSeriesUseCase(botDiscoveryReadPort);
    }

    @Test
    void shouldReturnSeriesWithNormalizedRange() {
        List<TimeSeriesPointSnapshot> expected = List.of(
                new TimeSeriesPointSnapshot(LocalDateTime.of(2026, 4, 1, 10, 0), 100.0)
        );
        when(botDiscoveryReadPort.listStrategyPerformanceSeries("stg_1", "1M")).thenReturn(expected);

        List<TimeSeriesPointSnapshot> result = useCase.execute("stg_1", "1m");

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void shouldThrowWhenRangeUnsupported() {
        assertThatThrownBy(() -> useCase.execute("stg_1", "3M"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported range: 3M");
    }
}

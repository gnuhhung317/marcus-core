package io.marcus.application.usecase;

import io.marcus.application.dto.BotAnalyticsDtos;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.BotBacktestRun;
import io.marcus.domain.model.BotDryRunPortfolioPoint;
import io.marcus.domain.port.BotBacktestPort;
import io.marcus.domain.port.BotDryRunPort;
import io.marcus.domain.repository.BotRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetBotAnalyticsUseCaseTest {

    private final BotRepository botRepository = mock(BotRepository.class);
    private final BotBacktestPort botBacktestPort = mock(BotBacktestPort.class);
    private final BotDryRunPort botDryRunPort = mock(BotDryRunPort.class);
    private final GetBotAnalyticsUseCase useCase = new GetBotAnalyticsUseCase(botRepository, botBacktestPort, botDryRunPort);

    @Test
    void shouldMergeLatestBacktestHistoricalCurveWithDryRunOutOfSampleCurve() {
        LocalDateTime t0 = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 2, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 1, 3, 0, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 1, 4, 0, 0);
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.of(Bot.builder().botId("bot_1").build()));
        when(botBacktestPort.findLatestRun("bot_1")).thenReturn(Optional.of(new BotBacktestRun("bt_1", "bot_1", "baseline", t0, t1, "{}", t1)));
        when(botBacktestPort.findPortfolioPoints("bot_1", "bt_1")).thenReturn(List.of(
                point("bot_1", t0, "1000"),
                point("bot_1", t1, "1100")
        ));
        when(botDryRunPort.findPortfolioPoints("bot_1")).thenReturn(List.of(
                point("bot_1", t2, "2000"),
                point("bot_1", t3, "2200")
        ));

        BotAnalyticsDtos.PerformanceSeriesResponse response = useCase.getPerformanceSeries("bot_1", "ALL");

        assertThat(response.splitTimestamp()).isEqualTo(t2);
        assertThat(response.points()).extracting(BotAnalyticsDtos.PerformancePoint::phase)
                .containsExactly("HISTORICAL", "HISTORICAL", "OUT_OF_SAMPLE", "OUT_OF_SAMPLE");
        assertThat(response.points()).extracting(BotAnalyticsDtos.PerformancePoint::value)
                .containsExactly(0.0, 10.0, 10.0, 20.0);
    }

    private BotDryRunPortfolioPoint point(String botId, LocalDateTime timestamp, String equity) {
        return new BotDryRunPortfolioPoint(
                botId,
                timestamp,
                BigDecimal.ZERO,
                new BigDecimal(equity),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}

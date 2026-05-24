package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListStrategyTradesUseCaseTest {

    @Mock
    private BotDiscoveryReadPort botDiscoveryReadPort;

    private ListStrategyTradesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListStrategyTradesUseCase(botDiscoveryReadPort);
    }

    @Test
    void shouldNormalizePaginationAndAssetFilter() {
        BotDiscoveryReadPort.TradeLogPageSnapshot page = new BotDiscoveryReadPort.TradeLogPageSnapshot(
                List.of(),
                0,
                100,
                0L
        );
        when(botDiscoveryReadPort.listStrategyTrades("stg_1", 0, 100, "BTCUSDT")).thenReturn(page);

        BotDiscoveryReadPort.TradeLogPageSnapshot result = useCase.execute(" stg_1 ", -1, 300, " btcusdt ");

        assertThat(result).isEqualTo(page);
    }
}

package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
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
    private TerminalReadPort terminalReadPort;

    private ListStrategyTradesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListStrategyTradesUseCase(terminalReadPort);
    }

    @Test
    void shouldNormalizePaginationAndAssetFilter() {
        TerminalReadPort.TradeLogPageSnapshot page = new TerminalReadPort.TradeLogPageSnapshot(
                List.of(),
                0,
                100,
                0L
        );
        when(terminalReadPort.listStrategyTrades("stg_1", 0, 100, "BTCUSDT")).thenReturn(page);

        TerminalReadPort.TradeLogPageSnapshot result = useCase.execute(" stg_1 ", -1, 300, " btcusdt ");

        assertThat(result).isEqualTo(page);
    }
}

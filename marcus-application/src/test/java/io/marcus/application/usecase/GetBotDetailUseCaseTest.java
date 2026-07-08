package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBotDetailUseCaseTest {

    @Mock
    private BotDiscoveryReadPort botDiscoveryReadPort;

    @InjectMocks
    private GetBotDetailUseCase getBotDetailUseCase;

    @Test
    void shouldNormalizeSourceBeforeReadingDetail() {
        BotDiscoveryReadPort.BotDetailSnapshot snapshot = new BotDiscoveryReadPort.BotDetailSnapshot(
                "bot_123",
                "Bot",
                "Description",
                "ACTIVE",
                "BTCUSDT",
                "BINANCE",
                "DRY_RUN",
                "dev_1",
                "ak_1",
                null,
                null,
                null
        );
        when(botDiscoveryReadPort.getBotDetail("bot_123", "DRY_RUN")).thenReturn(snapshot);

        BotDiscoveryReadPort.BotDetailSnapshot result = getBotDetailUseCase.execute(" bot_123 ", " dry_run ");

        assertSame(snapshot, result);
        verify(botDiscoveryReadPort).getBotDetail("bot_123", "DRY_RUN");
    }

    @Test
    void shouldRejectUnsupportedSource() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                getBotDetailUseCase.execute("bot_123", "SIGNAL_BASED")
        );

        assertEquals("Unsupported source: SIGNAL_BASED", thrown.getMessage());
        verifyNoInteractions(botDiscoveryReadPort);
    }
}

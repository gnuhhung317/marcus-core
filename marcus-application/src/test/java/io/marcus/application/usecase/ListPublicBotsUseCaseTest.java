package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPublicBotsUseCaseTest {

    @Mock
    private TerminalReadPort terminalReadPort;

    @InjectMocks
    private ListPublicBotsUseCase listPublicBotsUseCase;

    @Test
    void shouldNormalizeQueryFiltersAndPagination() {
        TerminalReadPort.BotDiscoveryPageSnapshot snapshot = new TerminalReadPort.BotDiscoveryPageSnapshot(
                List.of(),
                new TerminalReadPort.OffsetPaginationMetaSnapshot(0, 100, 0, 0, false)
        );
        when(terminalReadPort.listPublicBots("momentum", "BTCUSDT", "LOW", "-return", 0, 100))
                .thenReturn(snapshot);

        TerminalReadPort.BotDiscoveryPageSnapshot result = listPublicBotsUseCase.execute(
                " momentum ",
                " btcusdt ",
                " low ",
                null,
                -3,
                500
        );

        assertSame(snapshot, result);
        verify(terminalReadPort).listPublicBots("momentum", "BTCUSDT", "LOW", "-return", 0, 100);
    }

    @Test
    void shouldRejectUnsupportedRiskFilter() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, ()
                -> listPublicBotsUseCase.execute(null, null, "EXTREME", "-return", 0, 20)
        );

        org.junit.jupiter.api.Assertions.assertEquals("Unsupported risk: EXTREME", thrown.getMessage());
        verifyNoInteractions(terminalReadPort);
    }

    @Test
    void shouldRejectUnsupportedSortFilter() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, ()
                -> listPublicBotsUseCase.execute(null, null, "LOW", "alpha", 0, 20)
        );

        org.junit.jupiter.api.Assertions.assertEquals("Unsupported sort: alpha", thrown.getMessage());
        verifyNoInteractions(terminalReadPort);
    }
}

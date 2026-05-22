package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.port.UserProfileReadPort.OffsetPaginationMetaSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPublicBotsUseCaseTest {

    @Mock
    private BotDiscoveryReadPort botDiscoveryReadPort;

    @InjectMocks
    private ListPublicBotsUseCase listPublicBotsUseCase;

    @Test
    void shouldNormalizeQueryFiltersAndPagination() {
        BotDiscoveryReadPort.BotDiscoveryPageSnapshot snapshot = new BotDiscoveryReadPort.BotDiscoveryPageSnapshot(
                List.of(),
                new OffsetPaginationMetaSnapshot(0, 100, 0, 0, false)
        );
        when(botDiscoveryReadPort.listPublicBots("momentum", "BTCUSDT", "LOW", "-return", 0, 100))
                .thenReturn(snapshot);

        BotDiscoveryReadPort.BotDiscoveryPageSnapshot result = listPublicBotsUseCase.execute(
                " momentum ",
                " btcusdt ",
                " low ",
                null,
                -3,
                500
        );

        assertSame(snapshot, result);
        verify(botDiscoveryReadPort).listPublicBots("momentum", "BTCUSDT", "LOW", "-return", 0, 100);
    }

    @Test
    void shouldRejectUnsupportedRiskFilter() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                listPublicBotsUseCase.execute(null, null, "EXTREME", "-return", 0, 20)
        );

        assertEquals("Unsupported risk: EXTREME", thrown.getMessage());
        verifyNoInteractions(botDiscoveryReadPort);
    }

    @Test
    void shouldRejectUnsupportedSortFilter() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                listPublicBotsUseCase.execute(null, null, "LOW", "alpha", 0, 20)
        );

        assertEquals("Unsupported sort: alpha", thrown.getMessage());
        verifyNoInteractions(botDiscoveryReadPort);
    }
}

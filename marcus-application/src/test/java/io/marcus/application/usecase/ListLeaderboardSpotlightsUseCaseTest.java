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
class ListLeaderboardSpotlightsUseCaseTest {

    @Mock
    private BotDiscoveryReadPort botDiscoveryReadPort;

    private ListLeaderboardSpotlightsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListLeaderboardSpotlightsUseCase(botDiscoveryReadPort);
    }

    @Test
    void shouldReturnSpotlights() {
        List<BotDiscoveryReadPort.BotSpotlightSnapshot> spotlights = List.of(
                new BotDiscoveryReadPort.BotSpotlightSnapshot("bot_1", "Neutron", "CRYPTO", 0.03)
        );
        when(botDiscoveryReadPort.listLeaderboardSpotlights()).thenReturn(spotlights);

        List<BotDiscoveryReadPort.BotSpotlightSnapshot> result = useCase.execute();

        assertThat(result).containsExactlyElementsOf(spotlights);
    }
}

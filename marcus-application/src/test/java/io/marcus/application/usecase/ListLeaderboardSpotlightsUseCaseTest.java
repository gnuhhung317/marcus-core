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
class ListLeaderboardSpotlightsUseCaseTest {

    @Mock
    private TerminalReadPort terminalReadPort;

    private ListLeaderboardSpotlightsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListLeaderboardSpotlightsUseCase(terminalReadPort);
    }

    @Test
    void shouldReturnSpotlights() {
        List<TerminalReadPort.StrategySpotlightSnapshot> spotlights = List.of(
                new TerminalReadPort.StrategySpotlightSnapshot("stg_1", "Neutron", "CRYPTO", 0.03)
        );
        when(terminalReadPort.listLeaderboardSpotlights()).thenReturn(spotlights);

        List<TerminalReadPort.StrategySpotlightSnapshot> result = useCase.execute();

        assertThat(result).containsExactlyElementsOf(spotlights);
    }
}

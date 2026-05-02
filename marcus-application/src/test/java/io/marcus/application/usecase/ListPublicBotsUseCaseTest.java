package io.marcus.application.usecase;

<<<<<<< HEAD
import io.marcus.domain.port.TerminalReadPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
=======
import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.mapper.BotDtoMapper;
import io.marcus.domain.model.Bot;
import io.marcus.domain.repository.BotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
>>>>>>> 07cc74d5f615dfb2d511f1f2832e810f702e72e8
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
=======
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
>>>>>>> 07cc74d5f615dfb2d511f1f2832e810f702e72e8
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPublicBotsUseCaseTest {

    @Mock
<<<<<<< HEAD
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
=======
    private BotRepository botRepository;

    @Mock
    private BotDtoMapper botDtoMapper;

    private ListPublicBotsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListPublicBotsUseCase(botRepository, botDtoMapper);
    }

    @Test
    void shouldReturnMappedActiveBots() {
        Bot bot = Bot.builder().botId("bot_1").name("Public Bot").build();
        BotSummaryResult summary = BotSummaryResult.builder().botId("bot_1").botName("Public Bot").build();

        when(botRepository.findAllActive()).thenReturn(List.of(bot));
        when(botDtoMapper.toSummaryResult(bot, false)).thenReturn(summary);

        List<BotSummaryResult> result = useCase.execute();

        assertThat(result).containsExactly(summary);
        verify(botRepository).findAllActive();
        verify(botDtoMapper).toSummaryResult(bot, false);
>>>>>>> 07cc74d5f615dfb2d511f1f2832e810f702e72e8
    }
}

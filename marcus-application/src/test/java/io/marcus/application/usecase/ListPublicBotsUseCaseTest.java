package io.marcus.application.usecase;

import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.mapper.BotDtoMapper;
import io.marcus.domain.model.Bot;
import io.marcus.domain.repository.BotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPublicBotsUseCaseTest {

    @Mock
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
    }
}

package io.marcus.application.usecase;

import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.application.mapper.BotDtoMapper;
import io.marcus.domain.model.Bot;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.vo.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListDeveloperBotsUseCaseTest {

    @Mock
    private BotRepository botRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdentityService identityService;

    @Mock
    private BotDtoMapper botDtoMapper;

    @Mock
    private BotDiscoveryReadPort botDiscoveryReadPort;

    private ListDeveloperBotsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListDeveloperBotsUseCase(botRepository, userRepository, identityService, botDtoMapper, botDiscoveryReadPort);
    }

    @Test
    void shouldReturnMappedDeveloperBotsWhenAuthenticatedDeveloper() {
        Bot bot = Bot.builder().botId("bot_1").name("My Bot").developerId("dev_1").build();
        BotSummaryResult summary = BotSummaryResult.builder().botId("bot_1").botName("My Bot").apiKey("ak_1").build();
        BotDiscoveryReadPort.BotDetailSnapshot detail = new BotDiscoveryReadPort.BotDetailSnapshot(
                "bot_1", "My Bot", "desc", "ACTIVE", "BTCUSDT", "binance", "HISTORICAL", "dev_1", "ak_1",
                null, null, new BotDiscoveryReadPort.BotPerformanceSnapshot(0.15, 0.1, 1.5, 0.65, 0.15, 1.0)
        );

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("dev_1"));
        when(userRepository.existsByIdAndRole("dev_1", Role.DEVELOPER)).thenReturn(true);
        when(botRepository.findAllByDeveloperId("dev_1")).thenReturn(List.of(bot));
        when(botDiscoveryReadPort.getBotDetail("bot_1", "AUTO")).thenReturn(detail);
        when(botDtoMapper.toSummaryResult(bot, true, detail)).thenReturn(summary);

        List<BotSummaryResult> result = useCase.execute();

        assertThat(result).containsExactly(summary);
        verify(botRepository).findAllByDeveloperId("dev_1");
        verify(botDiscoveryReadPort).getBotDetail("bot_1", "AUTO");
        verify(botDtoMapper).toSummaryResult(bot, true, detail);
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute())
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");

        verifyNoInteractions(userRepository, botRepository, botDtoMapper, botDiscoveryReadPort);
    }

    @Test
    void shouldThrowWhenAuthenticatedUserIsNotDeveloper() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("trader_1"));
        when(userRepository.existsByIdAndRole("trader_1", Role.DEVELOPER)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute())
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only developer can list own bots");

        verifyNoInteractions(botRepository, botDtoMapper, botDiscoveryReadPort);
    }
}

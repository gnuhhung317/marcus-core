package io.marcus.application.usecase;

import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateBotStatusUseCaseTest {

    @Mock
    private BotRepository botRepository;

    @Mock
    private IdentityService identityService;

    @Mock
    private UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    private UpdateBotStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateBotStatusUseCase(botRepository, identityService, userSubscriptionPersistencePort);
    }

    @Test
    void shouldUpdateBotStatusWhenDeveloperIsOwnerAndNoSubscribers() {
        Bot bot = Bot.builder().botId("bot_1").developerId("dev_1").status(BotStatus.ACTIVE).build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("dev_1"));
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.of(bot));
        when(userSubscriptionPersistencePort.findActiveByBotId("bot_1")).thenReturn(Collections.emptyList());
        when(botRepository.save(any(Bot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bot updated = useCase.execute("bot_1", BotStatus.PAUSED);

        assertThat(updated.getStatus()).isEqualTo(BotStatus.PAUSED);
        verify(botRepository).save(bot);
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("bot_1", BotStatus.PAUSED))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");

        verifyNoInteractions(botRepository, userSubscriptionPersistencePort);
    }

    @Test
    void shouldThrowWhenBotNotFound() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("dev_1"));
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("bot_1", BotStatus.PAUSED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bot not found with id: bot_1");

        verifyNoMoreInteractions(botRepository);
        verifyNoInteractions(userSubscriptionPersistencePort);
    }

    @Test
    void shouldThrowWhenUserIsNotDeveloperOwner() {
        Bot bot = Bot.builder().botId("bot_1").developerId("dev_other").status(BotStatus.ACTIVE).build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("dev_1"));
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.of(bot));

        assertThatThrownBy(() -> useCase.execute("bot_1", BotStatus.PAUSED))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only the developer of the bot can modify its status");

        verify(botRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenStatusIsPausedAndHasSubscribers() {
        Bot bot = Bot.builder().botId("bot_1").developerId("dev_1").status(BotStatus.ACTIVE).build();
        UserSubscription sub = UserSubscription.builder().botId("bot_1").build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("dev_1"));
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.of(bot));
        when(userSubscriptionPersistencePort.findActiveByBotId("bot_1")).thenReturn(List.of(sub));

        assertThatThrownBy(() -> useCase.execute("bot_1", BotStatus.PAUSED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot pause/delete bot with active subscriptions");

        verify(botRepository, never()).save(any());
    }
}

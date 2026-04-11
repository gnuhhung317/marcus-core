package io.marcus.application.usecase;

import io.marcus.application.dto.SubscribeBotResult;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.BotSubscriberRoutingPort;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.Role;
import io.marcus.domain.vo.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscribeBotUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BotRepository botRepository;

    @Mock
    private UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    @Mock
    private BotSubscriberRoutingPort botSubscriberRoutingPort;

    private SubscribeBotUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SubscribeBotUseCase(
                identityService,
                userRepository,
                botRepository,
                userSubscriptionPersistencePort,
                botSubscriberRoutingPort
        );
    }

    @Test
    void shouldCreateSubscriptionWhenValidRequest() {
        Bot activeBot = Bot.builder().botId("bot_1").status(BotStatus.ACTIVE).build();
        UserSubscription savedSubscription = UserSubscription.builder()
                .userSubscriptionId("sub_1")
                .userId("usr_1")
                .botId("bot_1")
                .wsToken("ws_abc")
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now())
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.USER)).thenReturn(true);
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.of(activeBot));
        when(userSubscriptionPersistencePort.findActiveByUserIdAndBotId("usr_1", "bot_1")).thenReturn(Optional.empty());
        when(userSubscriptionPersistencePort.findAnyActiveWsTokenByUserId("usr_1")).thenReturn(Optional.of("ws_abc"));
        when(userSubscriptionPersistencePort.save(any(UserSubscription.class))).thenReturn(savedSubscription);

        SubscribeBotResult result = useCase.execute("bot_1");

        assertThat(result.botId()).isEqualTo("bot_1");
        assertThat(result.wsToken()).isEqualTo("ws_abc");
        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(botSubscriberRoutingPort).upsertSubscriber("bot_1", "usr_1");
    }

    @Test
    void shouldReuseExistingSubscriptionWhenAlreadySubscribed() {
        Bot activeBot = Bot.builder().botId("bot_1").status(BotStatus.ACTIVE).build();
        UserSubscription existingSubscription = UserSubscription.builder()
                .userSubscriptionId("sub_1")
                .userId("usr_1")
                .botId("bot_1")
                .wsToken("ws_existing")
                .status(SubscriptionStatus.ACTIVE)
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.USER)).thenReturn(true);
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.of(activeBot));
        when(userSubscriptionPersistencePort.findActiveByUserIdAndBotId("usr_1", "bot_1"))
                .thenReturn(Optional.of(existingSubscription));

        SubscribeBotResult result = useCase.execute("bot_1");

        assertThat(result.wsToken()).isEqualTo("ws_existing");
        verify(botSubscriberRoutingPort).upsertSubscriber("bot_1", "usr_1");
    }

    @Test
    void shouldThrowWhenBotIsNotActive() {
        Bot inactiveBot = Bot.builder().botId("bot_1").status(BotStatus.PAUSED).build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.USER)).thenReturn(true);
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.of(inactiveBot));

        assertThatThrownBy(() -> useCase.execute("bot_1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only active bot can be subscribed");
    }

    @Test
    void shouldThrowWhenBotNotFound() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.USER)).thenReturn(true);
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("bot_1"))
                .isInstanceOf(BotNotFoundException.class)
                .hasMessage("Bot not found: bot_1");
    }

    @Test
    void shouldThrowWhenCurrentUserIsMissing() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("bot_1"))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }

    @Test
    void shouldThrowWhenCurrentUserIsNotTrader() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.USER)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("bot_1"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only trader can subscribe bot");
    }
}

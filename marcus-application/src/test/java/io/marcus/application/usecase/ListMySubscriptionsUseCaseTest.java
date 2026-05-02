package io.marcus.application.usecase;

import io.marcus.application.dto.MySubscriptionsResult;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import io.marcus.domain.vo.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMySubscriptionsUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    @Mock
    private BotRepository botRepository;

    private ListMySubscriptionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListMySubscriptionsUseCase(identityService, userRepository, userSubscriptionPersistencePort, botRepository);
    }

    @Test
    void shouldListCurrentUserSubscriptions() {
        UserSubscription subscription = UserSubscription.builder()
                .userSubscriptionId("sub_1")
                .userId("usr_1")
                .botId("bot_1")
                .wsToken("ws_abc")
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now())
                .build();

        Bot bot = Bot.builder()
                .botId("bot_1")
                .name("Momentum Bot")
                .tradingPair("BTCUSDT")
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.USER)).thenReturn(true);
        when(userSubscriptionPersistencePort.findActiveByUserId("usr_1")).thenReturn(List.of(subscription));
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.of(bot));

        MySubscriptionsResult result = useCase.execute();

        assertThat(result.wsToken()).isEqualTo("ws_abc");
        assertThat(result.subscriptions()).hasSize(1);
        assertThat(result.subscriptions().get(0).botId()).isEqualTo("bot_1");
        assertThat(result.subscriptions().get(0).botName()).isEqualTo("Momentum Bot");
    }

    @Test
    void shouldAllowMissingBotRecordInSummary() {
        UserSubscription subscription = UserSubscription.builder()
                .userSubscriptionId("sub_1")
                .userId("usr_1")
                .botId("bot_missing")
                .wsToken("ws_abc")
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now())
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.USER)).thenReturn(true);
        when(userSubscriptionPersistencePort.findActiveByUserId("usr_1")).thenReturn(List.of(subscription));
        when(botRepository.findByBotId("bot_missing")).thenReturn(Optional.empty());

        MySubscriptionsResult result = useCase.execute();

        assertThat(result.subscriptions()).hasSize(1);
        assertThat(result.subscriptions().get(0).botName()).isNull();
    }

    @Test
    void shouldThrowWhenUserIsMissing() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute())
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }

    @Test
    void shouldThrowWhenUserIsNotTrader() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.USER)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute())
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only trader can view subscriptions");
    }
}

package io.marcus.application.usecase;

import io.marcus.application.dto.BotSubscriptionResult;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.exception.ResourceConflictException;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnsubscribeFromBotUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    private UnsubscribeFromBotUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UnsubscribeFromBotUseCase(identityService, userRepository, userSubscriptionPersistencePort);
    }

    @Test
    void shouldCancelActiveSubscription() {
        UserSubscription subscription = UserSubscription.builder()
                .userSubscriptionId("sub-1")
                .userId("usr_1")
                .botId("bot_1")
                .wsToken("ws-1")
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(5))
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.TRADER)).thenReturn(true);
        when(userSubscriptionPersistencePort.findActiveByUserIdAndBotId("usr_1", "bot_1")).thenReturn(Optional.of(subscription));

        BotSubscriptionResult result = useCase.execute("bot_1");

        assertThat(result.botId()).isEqualTo("bot_1");
        assertThat(result.status()).isEqualTo("UNSUBSCRIBED");
        verify(userSubscriptionPersistencePort).cancelActiveByUserIdAndBotId("usr_1", "bot_1");
    }

    @Test
    void shouldThrowConflictWhenSubscriptionIsNotActive() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.TRADER)).thenReturn(true);
        when(userSubscriptionPersistencePort.findActiveByUserIdAndBotId("usr_1", "bot_1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("bot_1"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("No active subscription found for bot: bot_1");
    }

    @Test
    void shouldThrowWhenUserIsMissing() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("bot_1"))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }

    @Test
    void shouldThrowWhenUserIsNotTrader() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userRepository.existsByIdAndRole("usr_1", Role.TRADER)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("bot_1"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only trader can unsubscribe from bot");
    }
}

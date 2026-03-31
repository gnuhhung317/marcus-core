package io.marcus.application.usecase;

import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.application.mapper.BotDtoMapper;
import io.marcus.domain.model.Bot;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.EncryptionService;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterBotUseCaseTest {

    @Mock
    private BotRepository botRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private IdentityService identityService;

    @Mock
    private UserRepository userRepository;

    private RegisterBotUseCase registerBotUseCase;

    @BeforeEach
    void setUp() {
        registerBotUseCase = new RegisterBotUseCase(
                botRepository,
                encryptionService,
                identityService,
                userRepository,
                new BotDtoMapper());
    }

    @Test
    void shouldRegisterBotWhenAuthenticatedDeveloper() {
        RegisterBotRequest request = new RegisterBotRequest(
                "Momentum bot",
                "BTCUSDT",
                "Alpha Trader",
                "binance");

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_001"));
        when(userRepository.existsByIdAndRole(eq("usr_001"), eq(Role.DEVELOPER))).thenReturn(true);
        when(encryptionService.encrypt(anyString())).thenAnswer(invocation -> "enc:" + invocation.getArgument(0, String.class));
        when(botRepository.save(any(Bot.class))).thenAnswer(invocation -> invocation.getArgument(0, Bot.class));

        BotRegistrationResult result = registerBotUseCase.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.botId()).startsWith("bot_");
        assertThat(result.apiKey()).startsWith("ak_");
        assertThat(result.rawSecret()).startsWith("sk_");
        assertThat(result.botName()).isEqualTo("Alpha Trader");
        assertThat(result.exchange()).isEqualTo("binance");
        assertThat(result.status()).isEqualTo("ACTIVE");

        ArgumentCaptor<Bot> botCaptor = ArgumentCaptor.forClass(Bot.class);
        verify(botRepository).save(botCaptor.capture());
        Bot savedBot = botCaptor.getValue();

        assertThat(savedBot.getDeveloperId()).isEqualTo("usr_001");
        assertThat(savedBot.getApiKey()).startsWith("ak_");
        assertThat(savedBot.getSecretKey()).startsWith("enc:sk_");
        assertThat(savedBot.getBotId()).startsWith("bot_");
    }

    @Test
    void shouldThrowUnauthenticatedWhenNoCurrentUser() {
        RegisterBotRequest request = new RegisterBotRequest(
                "Momentum bot",
                "BTCUSDT",
                "Alpha Trader",
                "binance");

        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerBotUseCase.execute(request))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");

        verifyNoInteractions(userRepository, botRepository, encryptionService);
    }

    @Test
    void shouldThrowForbiddenWhenUserIsNotDeveloper() {
        RegisterBotRequest request = new RegisterBotRequest(
                "Momentum bot",
                "BTCUSDT",
                "Alpha Trader",
                "binance");

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_001"));
        when(userRepository.existsByIdAndRole(eq("usr_001"), eq(Role.DEVELOPER))).thenReturn(false);

        assertThatThrownBy(() -> registerBotUseCase.execute(request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only developer can register bot");

        verify(botRepository, never()).save(any(Bot.class));
        verify(encryptionService, never()).encrypt(anyString());
    }

    @Test
    void shouldRejectNullRequest() {
        assertThatThrownBy(() -> registerBotUseCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Register bot request is required");

        verifyNoInteractions(identityService, userRepository, botRepository, encryptionService);
    }

    @Test
    void shouldRejectMissingBotName() {
        RegisterBotRequest request = new RegisterBotRequest(
                "Momentum bot",
                "BTCUSDT",
                " ",
                "binance");

        assertThatThrownBy(() -> registerBotUseCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bot name is required");

        verifyNoInteractions(identityService, userRepository, botRepository, encryptionService);
    }
}

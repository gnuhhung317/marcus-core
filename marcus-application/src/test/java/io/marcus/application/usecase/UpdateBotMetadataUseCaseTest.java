package io.marcus.application.usecase;

import io.marcus.application.dto.UpdateBotMetadataRequest;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.service.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateBotMetadataUseCaseTest {

    @Mock
    private BotRepository botRepository;

    @Mock
    private IdentityService identityService;

    private UpdateBotMetadataUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateBotMetadataUseCase(botRepository, identityService);
    }

    @Test
    void shouldUpdateBotMetadataWhenDeveloperIsOwner() {
        Bot bot = Bot.builder()
                .botId("bot_1")
                .developerId("dev_1")
                .name("Old Name")
                .description("Old Desc")
                .build();

        UpdateBotMetadataRequest request = new UpdateBotMetadataRequest(
                "New Name",
                "New Desc",
                "BTCUSDT",
                "binance",
                BigDecimal.TEN,
                "HIGH"
        );

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("dev_1"));
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.of(bot));
        when(botRepository.save(any(Bot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bot updated = useCase.execute("bot_1", request);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("New Desc");
        assertThat(updated.getTradingPair()).isEqualTo("BTCUSDT");
        assertThat(updated.getExchangeId()).isEqualTo("binance");
        assertThat(updated.getPrice()).isEqualTo(BigDecimal.TEN);
        assertThat(updated.getRiskLevel()).isEqualTo("HIGH");

        verify(botRepository).save(bot);
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        UpdateBotMetadataRequest request = new UpdateBotMetadataRequest(
                "New Name", null, null, null, null, null
        );
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("bot_1", request))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");

        verifyNoInteractions(botRepository);
    }

    @Test
    void shouldThrowWhenBotNotFound() {
        UpdateBotMetadataRequest request = new UpdateBotMetadataRequest(
                "New Name", null, null, null, null, null
        );
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("dev_1"));
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("bot_1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bot not found with id: bot_1");

        verifyNoMoreInteractions(botRepository);
    }

    @Test
    void shouldThrowWhenUserIsNotDeveloperOwner() {
        Bot bot = Bot.builder().botId("bot_1").developerId("dev_other").name("Old Name").build();
        UpdateBotMetadataRequest request = new UpdateBotMetadataRequest(
                "New Name", null, null, null, null, null
        );

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("dev_1"));
        when(botRepository.findByBotId("bot_1")).thenReturn(Optional.of(bot));

        assertThatThrownBy(() -> useCase.execute("bot_1", request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only the developer of the bot can modify its metadata");

        verify(botRepository, never()).save(any());
    }
}

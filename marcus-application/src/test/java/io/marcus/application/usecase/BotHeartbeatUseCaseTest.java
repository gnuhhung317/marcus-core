package io.marcus.application.usecase;

import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.RawEvent;
import io.marcus.domain.port.RawEventPersistencePort;
import io.marcus.domain.repository.BotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotHeartbeatUseCaseTest {

    @Mock
    private BotRepository botRepository;

    @Mock
    private RawEventPersistencePort rawEventPersistencePort;

    private BotHeartbeatUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BotHeartbeatUseCase(botRepository, rawEventPersistencePort);
    }

    @Test
    void shouldRegisterHeartbeatWhenCredentialsAreValid() {
        String botId = "bot-1";
        String apiKey = "apiKey-123";
        Bot bot = Bot.builder()
                .botId(botId)
                .apiKey(apiKey)
                .build();

        when(botRepository.findByBotId(botId)).thenReturn(Optional.of(bot));

        useCase.execute(botId, apiKey);

        ArgumentCaptor<RawEvent> eventCaptor = ArgumentCaptor.forClass(RawEvent.class);
        verify(rawEventPersistencePort).save(eventCaptor.capture());

        RawEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent).isNotNull();
        assertThat(savedEvent.getBotId()).isEqualTo(botId);
        assertThat(savedEvent.getType()).isEqualTo("heartbeat");
        assertThat(savedEvent.getSourceConnId()).isEqualTo("http-endpoint");
        assertThat(savedEvent.getProcessed()).isTrue();
        assertThat(savedEvent.getPayload()).containsKey("timestamp");
    }

    @Test
    void shouldThrowBotNotFoundExceptionWhenBotDoesNotExist() {
        String botId = "bot-invalid";
        String apiKey = "apiKey-123";

        when(botRepository.findByBotId(botId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(botId, apiKey))
                .isInstanceOf(BotNotFoundException.class)
                .hasMessageContaining("Bot not found");

        verifyNoInteractions(rawEventPersistencePort);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenApiKeyMismatches() {
        String botId = "bot-1";
        String correctApiKey = "correctKey";
        String wrongApiKey = "wrongKey";
        Bot bot = Bot.builder()
                .botId(botId)
                .apiKey(correctApiKey)
                .build();

        when(botRepository.findByBotId(botId)).thenReturn(Optional.of(bot));

        assertThatThrownBy(() -> useCase.execute(botId, wrongApiKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API Key mismatch");

        verifyNoInteractions(rawEventPersistencePort);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenInputsAreBlank() {
        assertThatThrownBy(() -> useCase.execute("", "key"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> useCase.execute("botId", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

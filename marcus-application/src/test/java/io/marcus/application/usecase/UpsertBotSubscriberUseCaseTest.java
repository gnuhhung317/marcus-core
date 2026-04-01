package io.marcus.application.usecase;

import io.marcus.application.dto.UpsertBotSubscriberRequest;
import io.marcus.domain.port.BotSubscriberRoutingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UpsertBotSubscriberUseCaseTest {

    @Mock
    private BotSubscriberRoutingPort botSubscriberRoutingPort;

    private UpsertBotSubscriberUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpsertBotSubscriberUseCase(botSubscriberRoutingPort);
    }

    @Test
    void shouldUpsertSubscriberWhenRequestIsValid() {
        UpsertBotSubscriberRequest request = new UpsertBotSubscriberRequest(" bot-1 ", " user-1 ");

        useCase.execute(request);

        verify(botSubscriberRoutingPort).upsertSubscriber("bot-1", "user-1");
    }

    @Test
    void shouldThrowWhenRequestIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Upsert bot subscriber request is required");

        verifyNoInteractions(botSubscriberRoutingPort);
    }

    @Test
    void shouldThrowWhenBotIdIsMissing() {
        UpsertBotSubscriberRequest request = new UpsertBotSubscriberRequest(" ", "user-1");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bot id is required");

        verifyNoInteractions(botSubscriberRoutingPort);
    }

    @Test
    void shouldThrowWhenUserIdIsMissing() {
        UpsertBotSubscriberRequest request = new UpsertBotSubscriberRequest("bot-1", " ");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User id is required");

        verifyNoInteractions(botSubscriberRoutingPort);
    }
}
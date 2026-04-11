package io.marcus.application.usecase;

import io.marcus.application.dto.RemoveBotSubscriberRequest;
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
class RemoveBotSubscriberUseCaseTest {

    @Mock
    private BotSubscriberRoutingPort botSubscriberRoutingPort;

    private RemoveBotSubscriberUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RemoveBotSubscriberUseCase(botSubscriberRoutingPort);
    }

    @Test
    void shouldRemoveSubscriberWhenRequestIsValid() {
        RemoveBotSubscriberRequest request = new RemoveBotSubscriberRequest(" bot-1 ", " user-1 ");

        useCase.execute(request);

        verify(botSubscriberRoutingPort).removeSubscriber("bot-1", "user-1");
    }

    @Test
    void shouldThrowWhenRequestIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Remove bot subscriber request is required");

        verifyNoInteractions(botSubscriberRoutingPort);
    }

    @Test
    void shouldThrowWhenBotIdIsMissing() {
        RemoveBotSubscriberRequest request = new RemoveBotSubscriberRequest(" ", "user-1");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bot id is required");

        verifyNoInteractions(botSubscriberRoutingPort);
    }

    @Test
    void shouldThrowWhenUserIdIsMissing() {
        RemoveBotSubscriberRequest request = new RemoveBotSubscriberRequest("bot-1", " ");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User id is required");

        verifyNoInteractions(botSubscriberRoutingPort);
    }
}
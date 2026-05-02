package io.marcus.application.usecase;

import io.marcus.application.dto.ResolveBotRoutingTargetsRequest;
import io.marcus.domain.port.BotSubscriberRoutingPort;
import io.marcus.domain.port.UserSessionRoutingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveBotRoutingTargetsUseCaseTest {

    @Mock
    private BotSubscriberRoutingPort botSubscriberRoutingPort;

    @Mock
    private UserSessionRoutingPort userSessionRoutingPort;

    private ResolveBotRoutingTargetsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResolveBotRoutingTargetsUseCase(botSubscriberRoutingPort, userSessionRoutingPort);
    }

    @Test
    void shouldResolveServerIdsWhenBotSubscribersExist() {
        ResolveBotRoutingTargetsRequest request = new ResolveBotRoutingTargetsRequest(" bot-1 ");

        when(botSubscriberRoutingPort.findActiveSubscriberUserIdsByBotId("bot-1"))
                .thenReturn(new LinkedHashSet<>(Arrays.asList(" user-1 ", "user-2", "user-1")));
        when(userSessionRoutingPort.findServerIdsByUserIds(anySet())).thenReturn(Set.of("ws-1", "ws-2"));

        Set<String> result = useCase.execute(request);

        assertThat(result).containsExactlyInAnyOrder("ws-1", "ws-2");
        verify(userSessionRoutingPort).findServerIdsByUserIds(new LinkedHashSet<>(Set.of("user-1", "user-2")));
    }

    @Test
    void shouldReturnEmptyWhenNoSubscribersFound() {
        ResolveBotRoutingTargetsRequest request = new ResolveBotRoutingTargetsRequest("bot-1");
        when(botSubscriberRoutingPort.findActiveSubscriberUserIdsByBotId("bot-1")).thenReturn(Set.of());

        Set<String> result = useCase.execute(request);

        assertThat(result).isEmpty();
        verifyNoInteractions(userSessionRoutingPort);
    }

    @Test
    void shouldThrowWhenRequestIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Resolve bot routing targets request is required");

        verifyNoInteractions(botSubscriberRoutingPort, userSessionRoutingPort);
    }

    @Test
    void shouldThrowWhenBotIdIsMissing() {
        ResolveBotRoutingTargetsRequest request = new ResolveBotRoutingTargetsRequest(" ");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bot id is required");

        verifyNoInteractions(botSubscriberRoutingPort, userSessionRoutingPort);
    }
}
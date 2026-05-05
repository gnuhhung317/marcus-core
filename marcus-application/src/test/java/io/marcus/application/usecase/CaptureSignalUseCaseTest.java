package io.marcus.application.usecase;

import io.marcus.application.dto.ResolveBotRoutingTargetsRequest;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.Signal;
import io.marcus.domain.port.SignalPublisherPort;
import io.marcus.domain.port.SignalServerDispatchPort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.SignalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaptureSignalUseCaseTest {

    @Mock
    private SignalRepository signalRepository;

    @Mock
    private BotRepository botRepository;

    @Mock
    private ResolveBotRoutingTargetsUseCase resolveBotRoutingTargetsUseCase;

    @Mock
    private SignalPublisherPort signalPublisherPort;

    @Mock
    private SignalServerDispatchPort signalServerDispatchPort;

    private CaptureSignalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CaptureSignalUseCase(
                signalRepository,
                botRepository,
                resolveBotRoutingTargetsUseCase,
                signalPublisherPort,
                signalServerDispatchPort
        );
    }

    @Test
    @DisplayName("Should publish and dispatch signal when routing targets exist")
    void shouldPublishAndDispatchSignalWhenRoutingTargetsExist() {
        Signal signal = Signal.builder()
                .signalId("signal-1")
                .botId("bot-1")
                .build();

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(new Bot())); // Bot exists
        when(signalRepository.existsBySignalId("signal-1"))
                .thenReturn(false); // Signal does not exist yet
        when(resolveBotRoutingTargetsUseCase.execute(new ResolveBotRoutingTargetsRequest("bot-1")))
                .thenReturn(Set.of("ws-1", "ws-2"));

        useCase.execute(signal);

        verify(signalRepository).save(signal);
        verify(signalPublisherPort).publish(signal);
        verify(signalServerDispatchPort).dispatchToServers(signal, Set.of("ws-1", "ws-2"));
    }

    @Test
    @DisplayName("Should publish only when no routing targets")
    void shouldPublishOnlyWhenNoRoutingTargets() {
        Signal signal = Signal.builder()
                .signalId("signal-1")
                .botId("bot-1")
                .build();

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(new Bot())); // Bot exists
        when(signalRepository.existsBySignalId("signal-1"))
                .thenReturn(false); // Signal does not exist yet
        when(resolveBotRoutingTargetsUseCase.execute(new ResolveBotRoutingTargetsRequest("bot-1")))
                .thenReturn(Set.of());

        useCase.execute(signal);

        verify(signalRepository).save(signal);
        verify(signalPublisherPort).publish(signal);
        verify(signalServerDispatchPort, never()).dispatchToServers(signal, Set.of());
    }

    @Test
    @DisplayName("Should throw when signal is null")
    void shouldThrowWhenSignalIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));

        assertEquals("Signal is required", exception.getMessage());
        verifyNoInteractions(signalRepository, resolveBotRoutingTargetsUseCase, signalServerDispatchPort);
    }

    @Test
    @DisplayName("Should throw when bot id is blank")
    void shouldThrowWhenBotIdIsBlank() {
        Signal signal = Signal.builder()
                .signalId("signal-1")
                .botId("   ")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(signal));

        assertEquals("Signal bot id is required", exception.getMessage());
        verifyNoInteractions(signalRepository, resolveBotRoutingTargetsUseCase, signalServerDispatchPort);
    }

    @Test
    @DisplayName("Should resolve targets using signal bot id")
    void shouldResolveTargetsUsingSignalBotId() {
        Signal signal = Signal.builder()
                .signalId("signal-1")
                .botId("bot-9")
                .build();
        when(botRepository.findByBotId("bot-9"))
                .thenReturn(Optional.of(new Bot())); // Bot exists
        when(signalRepository.existsBySignalId("signal-1"))
                .thenReturn(false); // Signal does not exist yet
        when(resolveBotRoutingTargetsUseCase.execute(new ResolveBotRoutingTargetsRequest("bot-9")))
                .thenReturn(Set.of());

        useCase.execute(signal);

        ArgumentCaptor<ResolveBotRoutingTargetsRequest> requestCaptor =
                ArgumentCaptor.forClass(ResolveBotRoutingTargetsRequest.class);
        verify(resolveBotRoutingTargetsUseCase).execute(requestCaptor.capture());
        assertEquals("bot-9", requestCaptor.getValue().botId());
    }

    @Test
    @DisplayName("Should throw when signal id is null")
    void shouldThrowWhenSignalIdIsNull() {
        Signal signal = Signal.builder()
                .signalId(null)
                .botId("bot-1")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(signal));

        assertEquals("Signal id is required", exception.getMessage());
        verifyNoInteractions(signalRepository, botRepository, resolveBotRoutingTargetsUseCase, signalServerDispatchPort);
    }

    @Test
    @DisplayName("Should throw when signal id is blank")
    void shouldThrowWhenSignalIdIsBlank() {
        Signal signal = Signal.builder()
                .signalId("   ")
                .botId("bot-1")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(signal));

        assertEquals("Signal id is required", exception.getMessage());
        verifyNoInteractions(signalRepository, botRepository, resolveBotRoutingTargetsUseCase, signalServerDispatchPort);
    }

    @Test
    @DisplayName("Should throw when bot id does not exist in database")
    void shouldThrowWhenBotIdDoesNotExist() {
        Signal signal = Signal.builder()
                .signalId("signal-1")
                .botId("demo-bot-id")
                .build();
        
        when(botRepository.findByBotId("demo-bot-id"))
                .thenReturn(Optional.empty()); // Bot does not exist

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(signal));

        assertEquals("Bot not found: demo-bot-id", exception.getMessage());
        verify(botRepository).findByBotId("demo-bot-id");
        verifyNoInteractions(signalRepository, resolveBotRoutingTargetsUseCase, signalServerDispatchPort);
    }

    @Test
    @DisplayName("Should publish and dispatch signal when all validations pass")
    void shouldPublishAndDispatchSignalWhenAllValidationsPass() {
        Signal signal = Signal.builder()
                .signalId("signal-1")
                .botId("bot-1")
                .build();

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(new Bot())); // Bot exists
        when(resolveBotRoutingTargetsUseCase.execute(new ResolveBotRoutingTargetsRequest("bot-1")))
                .thenReturn(Set.of("ws-1", "ws-2"));

        useCase.execute(signal);

        verify(botRepository).findByBotId("bot-1");
        verify(signalRepository).save(signal);
        verify(signalPublisherPort).publish(signal);
        verify(signalServerDispatchPort).dispatchToServers(signal, Set.of("ws-1", "ws-2"));
    }

    @Test
    @DisplayName("Should throw when signal id already exists (duplicate)")
    void shouldThrowWhenSignalIdAlreadyExists() {
        Signal signal = Signal.builder()
                .signalId("signal-1")
                .botId("bot-1")
                .build();

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(new Bot())); // Bot exists
        when(signalRepository.existsBySignalId("signal-1"))
                .thenReturn(true); // Signal already exists

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(signal));

        assertEquals("Signal already exists: signal-1", exception.getMessage());
        verify(signalRepository).existsBySignalId("signal-1");
        verify(signalRepository, never()).save(signal);
        verify(signalPublisherPort, never()).publish(signal);
        verifyNoInteractions(resolveBotRoutingTargetsUseCase, signalServerDispatchPort);
    }
}
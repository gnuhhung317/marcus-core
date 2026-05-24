package io.marcus.application.usecase;

import io.marcus.application.dto.CaptureSignalRequest;
import io.marcus.application.dto.ResolveBotRoutingTargetsRequest;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.Signal;
import io.marcus.domain.port.SignalPublisherPort;
import io.marcus.domain.port.SignalServerDispatchPort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.SignalRepository;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
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
        CaptureSignalRequest request = new CaptureSignalRequest(
                "signal-1",
                "bot-1",
                "BTCUSDT",
                SignalAction.OPEN_LONG,
                null,
                null,
                new BigDecimal("50000"),
                new BigDecimal("49000"),
                new BigDecimal("55000"),
                null,
                null,
                null,
                null,
                SignalStatus.RECEIVED,
                LocalDateTime.now(),
                "1h",
                new HashMap<>()
        );

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(new Bot()));
        when(signalRepository.existsBySignalId("signal-1"))
                .thenReturn(false);
        when(resolveBotRoutingTargetsUseCase.execute(new ResolveBotRoutingTargetsRequest("bot-1")))
                .thenReturn(Set.of("ws-1", "ws-2"));

        useCase.execute(request);

        ArgumentCaptor<Signal> signalCaptor = ArgumentCaptor.forClass(Signal.class);
        verify(signalRepository).save(signalCaptor.capture());
        Signal savedSignal = signalCaptor.getValue();
        assertEquals("signal-1", savedSignal.getSignalId());
        assertEquals("bot-1", savedSignal.getBotId());

        verify(signalPublisherPort).publish(savedSignal);
        verify(signalServerDispatchPort).dispatchToServers(savedSignal, Set.of("ws-1", "ws-2"));
    }

    @Test
    @DisplayName("Should publish only when no routing targets")
    void shouldPublishOnlyWhenNoRoutingTargets() {
        CaptureSignalRequest request = new CaptureSignalRequest(
                "signal-1",
                "bot-1",
                "BTCUSDT",
                SignalAction.OPEN_LONG,
                null,
                null,
                new BigDecimal("50000"),
                new BigDecimal("49000"),
                new BigDecimal("55000"),
                null,
                null,
                null,
                null,
                SignalStatus.RECEIVED,
                LocalDateTime.now(),
                "1h",
                new HashMap<>()
        );

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(new Bot()));
        when(signalRepository.existsBySignalId("signal-1"))
                .thenReturn(false);
        when(resolveBotRoutingTargetsUseCase.execute(new ResolveBotRoutingTargetsRequest("bot-1")))
                .thenReturn(Set.of());

        useCase.execute(request);

        ArgumentCaptor<Signal> signalCaptor = ArgumentCaptor.forClass(Signal.class);
        verify(signalRepository).save(signalCaptor.capture());
        Signal savedSignal = signalCaptor.getValue();

        verify(signalPublisherPort).publish(savedSignal);
        verify(signalServerDispatchPort, never()).dispatchToServers(any(), any());
    }

    @Test
    @DisplayName("Should throw when request is null")
    void shouldThrowWhenRequestIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));

        assertEquals("Signal request is required", exception.getMessage());
        verifyNoInteractions(signalRepository, resolveBotRoutingTargetsUseCase, signalServerDispatchPort);
    }

    @Test
    @DisplayName("Should resolve targets using signal bot id")
    void shouldResolveTargetsUsingSignalBotId() {
        CaptureSignalRequest request = new CaptureSignalRequest(
                "signal-1",
                "bot-9",
                "BTCUSDT",
                SignalAction.OPEN_LONG,
                null,
                null,
                new BigDecimal("50000"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(botRepository.findByBotId("bot-9"))
                .thenReturn(Optional.of(new Bot()));
        when(signalRepository.existsBySignalId("signal-1"))
                .thenReturn(false);
        when(resolveBotRoutingTargetsUseCase.execute(new ResolveBotRoutingTargetsRequest("bot-9")))
                .thenReturn(Set.of());

        useCase.execute(request);

        ArgumentCaptor<ResolveBotRoutingTargetsRequest> requestCaptor
                = ArgumentCaptor.forClass(ResolveBotRoutingTargetsRequest.class);
        verify(resolveBotRoutingTargetsUseCase).execute(requestCaptor.capture());
        assertEquals("bot-9", requestCaptor.getValue().botId());
    }

    @Test
    @DisplayName("Should throw when bot id does not exist in database")
    void shouldThrowWhenBotIdDoesNotExist() {
        CaptureSignalRequest request = new CaptureSignalRequest(
                "signal-1",
                "demo-bot-id",
                "BTCUSDT",
                SignalAction.OPEN_LONG,
                null,
                null,
                new BigDecimal("50000"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(botRepository.findByBotId("demo-bot-id"))
                .thenReturn(Optional.empty());

        io.marcus.domain.exception.BotNotFoundException exception = assertThrows(io.marcus.domain.exception.BotNotFoundException.class, () -> useCase.execute(request));

        assertEquals("Bot not found: demo-bot-id", exception.getMessage());
        verify(botRepository).findByBotId("demo-bot-id");
        verifyNoInteractions(signalRepository, resolveBotRoutingTargetsUseCase, signalServerDispatchPort);
    }

    @Test
    @DisplayName("Should publish and dispatch signal when all validations pass")
    void shouldPublishAndDispatchSignalWhenAllValidationsPass() {
        CaptureSignalRequest request = new CaptureSignalRequest(
                "signal-1",
                "bot-1",
                "BTCUSDT",
                SignalAction.OPEN_LONG,
                null,
                null,
                new BigDecimal("50000"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(new Bot()));
        when(resolveBotRoutingTargetsUseCase.execute(new ResolveBotRoutingTargetsRequest("bot-1")))
                .thenReturn(Set.of("ws-1", "ws-2"));

        useCase.execute(request);

        verify(botRepository).findByBotId("bot-1");
        verify(signalRepository).save(any(Signal.class));
        verify(signalPublisherPort).publish(any(Signal.class));
        verify(signalServerDispatchPort).dispatchToServers(any(Signal.class), org.mockito.ArgumentMatchers.eq(Set.of("ws-1", "ws-2")));
    }

    @Test
    @DisplayName("Should throw when signal id already exists (duplicate)")
    void shouldThrowWhenSignalIdAlreadyExists() {
        CaptureSignalRequest request = new CaptureSignalRequest(
                "signal-1",
                "bot-1",
                "BTCUSDT",
                SignalAction.OPEN_LONG,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(new Bot()));
        when(signalRepository.existsBySignalId("signal-1"))
                .thenReturn(true);

        io.marcus.domain.exception.DuplicateSignalException exception = assertThrows(io.marcus.domain.exception.DuplicateSignalException.class, () -> useCase.execute(request));

        assertEquals("Signal already exists: signal-1", exception.getMessage());
        verify(signalRepository).existsBySignalId("signal-1");
        verify(signalRepository, never()).save(any());
        verify(signalPublisherPort, never()).publish(any());
        verifyNoInteractions(resolveBotRoutingTargetsUseCase, signalServerDispatchPort);
    }

    @Test
    @DisplayName("Should bypass publish and dispatch when signal is simulated")
    void shouldBypassPublishAndDispatchWhenSignalIsSimulated() {
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("simulation", true);

        CaptureSignalRequest request = new CaptureSignalRequest(
                "signal-sim-1",
                "bot-1",
                "BTCUSDT",
                SignalAction.OPEN_LONG,
                null,
                null,
                new BigDecimal("50000"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                metadata
        );

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(new Bot()));
        when(signalRepository.existsBySignalId("signal-sim-1"))
                .thenReturn(false);

        useCase.execute(request);

        ArgumentCaptor<Signal> signalCaptor = ArgumentCaptor.forClass(Signal.class);
        verify(signalRepository).save(signalCaptor.capture());
        Signal savedSignal = signalCaptor.getValue();
        assertEquals("signal-sim-1", savedSignal.getSignalId());
        assertEquals(true, savedSignal.simulated());

        verify(signalPublisherPort, never()).publish(any());
        verify(signalServerDispatchPort, never()).dispatchToServers(any(), any());
    }
}

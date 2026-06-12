package io.marcus.application.usecase;

import io.marcus.application.dto.CaptureSignalRequest;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.Signal;
import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.port.SignalPublisherPort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.SignalRepository;
import io.marcus.domain.vo.BotStatus;
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
    private SignalPublisherPort signalPublisherPort;

    private CaptureSignalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CaptureSignalUseCase(signalRepository, botRepository, signalPublisherPort);
    }

    @Test
    @DisplayName("Should save and publish signal when validations pass")
    void shouldSaveAndPublishSignalWhenValid() {
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
                .thenReturn(Optional.of(Bot.builder().status(BotStatus.ACTIVE).build()));
        when(signalRepository.existsBySignalId("signal-1"))
                .thenReturn(false);

        useCase.execute(request);

        ArgumentCaptor<Signal> signalCaptor = ArgumentCaptor.forClass(Signal.class);
        verify(signalRepository).save(signalCaptor.capture());
        Signal savedSignal = signalCaptor.getValue();
        assertEquals("signal-1", savedSignal.getSignalId());
        assertEquals("bot-1", savedSignal.getBotId());
        assertEquals("BTCUSDT", savedSignal.getSymbol());
        assertEquals(SignalAction.OPEN_LONG, savedSignal.getAction());
        assertEquals(new BigDecimal("50000"), savedSignal.getEntry());
        assertEquals(new BigDecimal("49000"), savedSignal.getStopLoss());
        assertEquals(new BigDecimal("55000"), savedSignal.getTakeProfit());
        assertEquals(SignalStatus.RECEIVED, savedSignal.getStatus());

        verify(signalPublisherPort).publish(savedSignal);
    }

    @Test
    @DisplayName("Should bypass publish when signal is simulated")
    void shouldBypassPublishWhenSignalIsSimulated() {
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
                SignalStatus.RECEIVED,
                LocalDateTime.now(),
                "1h",
                metadata
        );

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(Bot.builder().status(BotStatus.ACTIVE).build()));
        when(signalRepository.existsBySignalId("signal-sim-1"))
                .thenReturn(false);

        useCase.execute(request);

        ArgumentCaptor<Signal> signalCaptor = ArgumentCaptor.forClass(Signal.class);
        verify(signalRepository).save(signalCaptor.capture());
        assertEquals("signal-sim-1", signalCaptor.getValue().getSignalId());
        assertEquals(true, signalCaptor.getValue().simulated());
        verify(signalPublisherPort, never()).publish(any());
    }

    @Test
    @DisplayName("Should throw when request is null")
    void shouldThrowWhenRequestIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));

        assertEquals("Signal request is required", exception.getMessage());
        verifyNoInteractions(signalRepository, botRepository, signalPublisherPort);
    }

    @Test
    @DisplayName("Should throw when bot is missing")
    void shouldThrowWhenBotIsMissing() {
        CaptureSignalRequest request = new CaptureSignalRequest(
                "signal-1",
                "bot-missing",
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
                SignalStatus.RECEIVED,
                LocalDateTime.now(),
                "1h",
                new HashMap<>()
        );

        when(botRepository.findByBotId("bot-missing"))
                .thenReturn(Optional.empty());

        BotNotFoundException exception = assertThrows(BotNotFoundException.class, () -> useCase.execute(request));

        assertEquals("Bot not found: bot-missing", exception.getMessage());
        verify(botRepository).findByBotId("bot-missing");
        verifyNoInteractions(signalRepository, signalPublisherPort);
    }

    @Test
    @DisplayName("Should throw when signal already exists")
    void shouldThrowWhenSignalAlreadyExists() {
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
                SignalStatus.RECEIVED,
                LocalDateTime.now(),
                "1h",
                new HashMap<>()
        );

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(Bot.builder().status(BotStatus.ACTIVE).build()));
        when(signalRepository.existsBySignalId("signal-1"))
                .thenReturn(true);

        io.marcus.domain.exception.DuplicateSignalException exception = assertThrows(
                io.marcus.domain.exception.DuplicateSignalException.class,
                () -> useCase.execute(request));

        assertEquals("Signal already exists: signal-1", exception.getMessage());
        verify(signalRepository).existsBySignalId("signal-1");
        verify(signalRepository, never()).save(any());
        verify(signalPublisherPort, never()).publish(any());
    }

    @Test
    @DisplayName("Should reject signal when bot is paused")
    void shouldRejectSignalWhenBotIsPaused() {
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
                SignalStatus.RECEIVED,
                LocalDateTime.now(),
                "1h",
                new HashMap<>()
        );

        when(botRepository.findByBotId("bot-1"))
                .thenReturn(Optional.of(Bot.builder().status(BotStatus.PAUSED).build()));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> useCase.execute(request));

        assertEquals("Only active bot can publish signals", exception.getMessage());
        verify(signalRepository, never()).save(any());
        verify(signalPublisherPort, never()).publish(any());
    }
}

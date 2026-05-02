package io.marcus.infrastructure.messaging;

import io.marcus.domain.model.Signal;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.mapper.SignalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaSignalStorageConsumerAdapterTest {

    @Mock
    private SpringDataSignalRepository springDataSignalRepository;

    @Mock
    private SignalMapper signalMapper;

    private KafkaSignalStorageConsumerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaSignalStorageConsumerAdapter(springDataSignalRepository, signalMapper, 2);
    }

    @Test
    void shouldPersistBatchWhenBatchSizeReached() {
        Signal firstSignal = Signal.builder().signalId("signal-1").botId("bot-1").build();
        Signal secondSignal = Signal.builder().signalId("signal-2").botId("bot-1").build();
        SignalEntity firstEntity = SignalEntity.builder().signalId("signal-1").botId("bot-1").build();
        SignalEntity secondEntity = SignalEntity.builder().signalId("signal-2").botId("bot-1").build();

        when(signalMapper.toEntity(firstSignal)).thenReturn(firstEntity);
        when(signalMapper.toEntity(secondSignal)).thenReturn(secondEntity);

        adapter.consume(firstSignal);

        verify(springDataSignalRepository, never()).saveAll(anyList());

        adapter.consume(secondSignal);

        ArgumentCaptor<List<SignalEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(springDataSignalRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(firstEntity, secondEntity);
    }

    @Test
    void shouldPersistPendingSignalsWhenFlushed() {
        adapter = new KafkaSignalStorageConsumerAdapter(springDataSignalRepository, signalMapper, 3);

        Signal firstSignal = Signal.builder().signalId("signal-1").botId("bot-1").build();
        Signal secondSignal = Signal.builder().signalId("signal-2").botId("bot-1").build();
        SignalEntity firstEntity = SignalEntity.builder().signalId("signal-1").botId("bot-1").build();
        SignalEntity secondEntity = SignalEntity.builder().signalId("signal-2").botId("bot-1").build();

        when(signalMapper.toEntity(firstSignal)).thenReturn(firstEntity);
        when(signalMapper.toEntity(secondSignal)).thenReturn(secondEntity);

        adapter.consume(firstSignal);
        adapter.consume(secondSignal);

        verify(springDataSignalRepository, never()).saveAll(anyList());

        adapter.flushPending();

        ArgumentCaptor<List<SignalEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(springDataSignalRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(firstEntity, secondEntity);
    }

    @Test
    void shouldSkipNullSignalAndUnmappableSignal() {
        Signal signal = Signal.builder().signalId("signal-1").botId("bot-1").build();
        when(signalMapper.toEntity(signal)).thenReturn(null);

        adapter.consume(null);
        adapter.consume(signal);
        adapter.flushPending();

        verifyNoInteractions(springDataSignalRepository);
    }
}

package io.marcus.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SpringDataSignalRepository springDataSignalRepository;

    @Mock
    private SignalMapper signalMapper;

    private KafkaSignalStorageConsumerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaSignalStorageConsumerAdapter(objectMapper, springDataSignalRepository, signalMapper, 2);
    }

    @Test
    void shouldPersistBatchWhenBatchSizeReached() {
        Signal firstSignal = signal("signal-1", "bot-1");
        Signal secondSignal = signal("signal-2", "bot-1");

        SignalEntity firstEntity = signalEntity("signal-1", "bot-1");
        SignalEntity secondEntity = signalEntity("signal-2", "bot-1");

        when(signalMapper.toEntity(firstSignal)).thenReturn(firstEntity);
        when(signalMapper.toEntity(secondSignal)).thenReturn(secondEntity);

        adapter.consume("{\"signalId\":\"signal-1\",\"botId\":\"bot-1\"}");

        verify(springDataSignalRepository, never()).saveAll(anyList());

        adapter.consume("{\"signalId\":\"signal-2\",\"botId\":\"bot-1\"}");

        ArgumentCaptor<List<SignalEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(springDataSignalRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(firstEntity, secondEntity);
    }

    @Test
    void shouldPersistPendingSignalsWhenFlushed() {
        adapter = new KafkaSignalStorageConsumerAdapter(objectMapper, springDataSignalRepository, signalMapper, 3);

        Signal firstSignal = signal("signal-1", "bot-1");
        Signal secondSignal = signal("signal-2", "bot-1");

        SignalEntity firstEntity = signalEntity("signal-1", "bot-1");
        SignalEntity secondEntity = signalEntity("signal-2", "bot-1");

        when(signalMapper.toEntity(firstSignal)).thenReturn(firstEntity);
        when(signalMapper.toEntity(secondSignal)).thenReturn(secondEntity);

        adapter.consume("{\"signalId\":\"signal-1\",\"botId\":\"bot-1\"}");
        adapter.consume("{\"signalId\":\"signal-2\",\"botId\":\"bot-1\"}");

        verify(springDataSignalRepository, never()).saveAll(anyList());

        adapter.flushPending();

        ArgumentCaptor<List<SignalEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(springDataSignalRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(firstEntity, secondEntity);
    }

    @Test
    void shouldSkipNullSignalAndUnmappableSignal() {
        Signal signal = signal("signal-1", "bot-1");
        when(signalMapper.toEntity(signal)).thenReturn(null);

        adapter.consume(null);
        adapter.consume("{\"signalId\":\"signal-1\",\"botId\":\"bot-1\"}");
        adapter.flushPending();

        verifyNoInteractions(springDataSignalRepository);
    }

    private static Signal signal(String signalId, String botId) {
        Signal signal = new Signal();
        signal.setSignalId(signalId);
        signal.setBotId(botId);
        return signal;
    }

    private static SignalEntity signalEntity(String signalId, String botId) {
        SignalEntity signalEntity = new SignalEntity();
        signalEntity.setSignalId(signalId);
        signalEntity.setBotId(botId);
        return signalEntity;
    }
}

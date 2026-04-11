package io.marcus.infrastructure.messaging;

import io.marcus.domain.model.Signal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaSignalServerDispatchAdapterTest {

    @Mock
    private KafkaTemplate<String, Signal> kafkaTemplate;

    private KafkaSignalServerDispatchAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaSignalServerDispatchAdapter(kafkaTemplate, "routing-topic");
    }

    @Test
    void shouldDispatchToEachValidServerId() {
        Signal signal = Signal.builder()
                .signalId("signal-1")
                .botId("bot-1")
                .build();

        adapter.dispatchToServers(signal, Set.of(" ws-1 ", "", "ws-2"));

        verify(kafkaTemplate).send("routing-topic", "ws-1", signal);
        verify(kafkaTemplate).send("routing-topic", "ws-2", signal);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    void shouldSkipDispatchWhenSignalOrTargetsInvalid() {
        Signal signal = Signal.builder()
                .signalId("signal-1")
                .botId("bot-1")
                .build();

        adapter.dispatchToServers(null, Set.of("ws-1"));
        adapter.dispatchToServers(signal, null);
        adapter.dispatchToServers(signal, Set.of());

        verifyNoInteractions(kafkaTemplate);
    }
}

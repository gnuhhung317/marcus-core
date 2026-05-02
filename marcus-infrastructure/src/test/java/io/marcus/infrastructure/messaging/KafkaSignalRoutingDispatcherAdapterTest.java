package io.marcus.infrastructure.messaging;

import io.marcus.domain.model.Signal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaSignalRoutingDispatcherAdapterTest {

    @Mock
    private RedisSignalDispatchPublisherAdapter redisSignalDispatchPublisherAdapter;

    private KafkaSignalRoutingDispatcherAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaSignalRoutingDispatcherAdapter(redisSignalDispatchPublisherAdapter);
    }

    @Test
    void shouldForwardSignalToRedisPublisherWithSanitizedTargetServer() {
        Signal signal = Signal.builder().signalId("signal-1").botId("bot-1").build();

        adapter.dispatchToRedis(signal, " ws-1 ");

        ArgumentCaptor<RoutingDispatchMessage> captor = ArgumentCaptor.forClass(RoutingDispatchMessage.class);
        verify(redisSignalDispatchPublisherAdapter).publish(captor.capture());
        assertThat(captor.getValue().targetServerId()).isEqualTo("ws-1");
        assertThat(captor.getValue().signal()).isEqualTo(signal);
    }

    @Test
    void shouldSkipWhenSignalOrTargetServerIsMissing() {
        Signal signal = Signal.builder().signalId("signal-1").botId("bot-1").build();

        adapter.dispatchToRedis(null, "ws-1");
        adapter.dispatchToRedis(signal, null);
        adapter.dispatchToRedis(signal, " ");

        verifyNoInteractions(redisSignalDispatchPublisherAdapter);
    }
}
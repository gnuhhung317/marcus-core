package io.marcus.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.Signal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSignalDispatchPublisherAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private RedisSignalDispatchPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisSignalDispatchPublisherAdapter(
                stringRedisTemplate,
                objectMapper,
                "marcus:signals:dispatch:broadcast"
        );
    }

    @Test
    void shouldPublishSerializedDispatchMessageToRedisChannel() throws Exception {
        Signal signal = Signal.builder().signalId("signal-1").botId("bot-1").build();
        RoutingDispatchMessage dispatchMessage = new RoutingDispatchMessage("ws-1", signal);

        when(objectMapper.writeValueAsString(dispatchMessage))
                .thenReturn("{\"targetServerId\":\"ws-1\",\"signal\":{\"signalId\":\"signal-1\"}} ");

        adapter.publish(dispatchMessage);

        verify(stringRedisTemplate).convertAndSend(
                "marcus:signals:dispatch:broadcast",
                "{\"targetServerId\":\"ws-1\",\"signal\":{\"signalId\":\"signal-1\"}} "
        );
    }

    @Test
    void shouldThrowWhenSerializationFails() throws Exception {
        Signal signal = Signal.builder().signalId("signal-1").botId("bot-1").build();
        RoutingDispatchMessage dispatchMessage = new RoutingDispatchMessage("ws-1", signal);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("serialize failed") {
                });

        assertThatThrownBy(() -> adapter.publish(dispatchMessage))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize routing dispatch message");
    }

    @Test
    void shouldSkipWhenDispatchMessageInvalid() {
        adapter.publish(null);
        adapter.publish(new RoutingDispatchMessage(" ", Signal.builder().signalId("signal-1").build()));

        verifyNoInteractions(stringRedisTemplate, objectMapper);
    }
}
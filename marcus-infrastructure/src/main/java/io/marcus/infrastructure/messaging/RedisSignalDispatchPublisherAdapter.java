package io.marcus.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisSignalDispatchPublisherAdapter {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final String dispatchBroadcastChannel;

    public RedisSignalDispatchPublisherAdapter(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${marcus.messaging.signal-dispatch-broadcast-channel:marcus:signals:dispatch:broadcast}") String dispatchBroadcastChannel
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.dispatchBroadcastChannel = dispatchBroadcastChannel;
    }

    public void publish(RoutingDispatchMessage dispatchMessage) {
        if (dispatchMessage == null
                || dispatchMessage.signal() == null
                || dispatchMessage.targetServerId() == null
                || dispatchMessage.targetServerId().isBlank()) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(dispatchMessage);
            stringRedisTemplate.convertAndSend(dispatchBroadcastChannel, payload);
            log.debug("Published signal {} to Redis channel {} for target server {}",
                    dispatchMessage.signal().getSignalId(),
                    dispatchBroadcastChannel,
                    dispatchMessage.targetServerId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize routing dispatch message", exception);
        }
    }
}
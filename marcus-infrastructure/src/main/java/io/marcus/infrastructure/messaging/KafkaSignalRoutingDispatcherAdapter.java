package io.marcus.infrastructure.messaging;

import io.marcus.domain.model.Signal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaSignalRoutingDispatcherAdapter {

    private final RedisSignalDispatchPublisherAdapter redisSignalDispatchPublisherAdapter;

    public KafkaSignalRoutingDispatcherAdapter(RedisSignalDispatchPublisherAdapter redisSignalDispatchPublisherAdapter) {
        this.redisSignalDispatchPublisherAdapter = redisSignalDispatchPublisherAdapter;
    }

    @KafkaListener(
            topics = "${marcus.messaging.signal-routing-topic:trading-signals-routing}",
            groupId = "${marcus.messaging.signal-dispatcher-group:marcus-signal-dispatcher}"
    )
    public void dispatchToRedis(
            Signal signal,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String targetServerId
    ) {
        if (signal == null || targetServerId == null || targetServerId.isBlank()) {
            log.warn("Skipping routing dispatch because signal or target server id is missing");
            return;
        }

        String sanitizedTargetServerId = targetServerId.trim();
        redisSignalDispatchPublisherAdapter.publish(new RoutingDispatchMessage(sanitizedTargetServerId, signal));
        log.info("Dispatched signal {} from broker to Redis Pub/Sub for target server {}",
                signal.getSignalId(),
                sanitizedTargetServerId);
    }
}
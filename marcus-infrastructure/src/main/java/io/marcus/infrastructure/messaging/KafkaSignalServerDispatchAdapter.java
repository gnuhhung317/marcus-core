package io.marcus.infrastructure.messaging;

import io.marcus.domain.model.Signal;
import io.marcus.domain.port.SignalServerDispatchPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class KafkaSignalServerDispatchAdapter implements SignalServerDispatchPort {

    private final KafkaTemplate<String, Signal> kafkaTemplate;
    private final String signalRoutingTopic;

    public KafkaSignalServerDispatchAdapter(
            KafkaTemplate<String, Signal> kafkaTemplate,
            @Value("${marcus.messaging.signal-routing-topic:trading-signals-routing}") String signalRoutingTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.signalRoutingTopic = signalRoutingTopic;
    }

    @Override
    public void dispatchToServers(Signal signal, Set<String> serverIds) {
        if (signal == null || serverIds == null || serverIds.isEmpty()) {
            return;
        }

        int dispatchedCount = 0;
        for (String serverId : serverIds) {
            if (serverId == null || serverId.isBlank()) {
                continue;
            }

            kafkaTemplate.send(signalRoutingTopic, serverId.trim(), signal);
            dispatchedCount++;
        }

        log.info("Dispatched signal {} to {} routing targets", signal.getSignalId(), dispatchedCount);
    }
}
package io.marcus.infrastructure.messaging;

import io.marcus.domain.model.Signal;
import io.marcus.domain.repository.SignalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaSignalProducer implements SignalRepository {

    private final KafkaTemplate<String, Signal> kafkaTemplate;
    private final String signalTopic;

    public KafkaSignalProducer(
            KafkaTemplate<String, Signal> kafkaTemplate,
            @Value("${marcus.messaging.signal-storage-topic:trading-signals}") String signalTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.signalTopic = signalTopic;
    }

    @Override
    public void publish(Signal signal) {
        log.info("Publishing signal: {}", signal.getSignalId());
        kafkaTemplate.send(signalTopic, signal.getBotId(), signal);
    }
}

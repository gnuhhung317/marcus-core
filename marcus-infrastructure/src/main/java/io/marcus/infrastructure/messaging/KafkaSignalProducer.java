package io.marcus.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.Signal;
import io.marcus.domain.repository.SignalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaSignalProducer implements SignalRepository {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String signalTopic;

    public KafkaSignalProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${marcus.messaging.signal-storage-topic:trading-signals}") String signalTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.signalTopic = signalTopic;
    }

    @Override
    public void publish(Signal signal) {
        log.info("Publishing signal: {}", signal.getSignalId());
        try {
            kafkaTemplate.send(signalTopic, signal.getBotId(), objectMapper.writeValueAsString(signal));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize signal for Kafka publish", exception);
        }
    }
}

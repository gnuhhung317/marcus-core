package io.marcus.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.Signal;
import io.marcus.domain.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaSignalProducer implements SignalRepository {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String SIGNAL_TOPIC = "trading-signals";

    @Override
    public void publish(Signal signal) {
        log.info("Publishing signal: {}", signal.getSignalId());
        try {
            kafkaTemplate.send(SIGNAL_TOPIC, signal.getBotId(), objectMapper.writeValueAsString(signal));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize signal for Kafka publish", exception);
        }
    }
}

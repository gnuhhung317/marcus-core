package io.marcus.infrastructure.messaging;

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
    private final KafkaTemplate<String, Signal> kafkaTemplate;
    private final String SIGNAL_TOPIC = "trading-signals";

    @Override
    public void publish(Signal signal) {
        log.info("Publishing signal: {}", signal.getSignalId());
        kafkaTemplate.send(SIGNAL_TOPIC, signal.getBotId(), signal);
    }
}

package io.marcus.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.Signal;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.mapper.SignalMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class KafkaSignalStorageConsumerAdapter {

    private final ObjectMapper objectMapper;
    private final SpringDataSignalRepository springDataSignalRepository;
    private final SignalMapper signalMapper;
    private final int batchSize;

    private final List<SignalEntity> pendingEntities = new ArrayList<>();
    private final Object monitor = new Object();

    public KafkaSignalStorageConsumerAdapter(
            ObjectMapper objectMapper,
            SpringDataSignalRepository springDataSignalRepository,
            SignalMapper signalMapper,
            @Value("${marcus.messaging.signal-storage-batch-size:100}") int batchSize
    ) {
        this.objectMapper = objectMapper;
        this.springDataSignalRepository = springDataSignalRepository;
        this.signalMapper = signalMapper;
        this.batchSize = Math.max(batchSize, 1);
    }

    @KafkaListener(
            topics = "${marcus.messaging.signal-storage-topic:trading-signals}",
            groupId = "${marcus.messaging.signal-storage-group:marcus-signal-storage}"
    )
    public void consume(String signalJson) {
        if (signalJson == null || signalJson.isBlank()) {
            log.warn("Skipping blank signal payload from storage topic");
            return;
        }

        Signal signal;
        try {
            signal = objectMapper.readValue(signalJson, Signal.class);
        } catch (Exception exception) {
            log.warn("Skipping signal payload from storage topic because it could not be parsed: {}", exception.getMessage());
            return;
        }

        SignalEntity signalEntity = signalMapper.toEntity(signal);
        if (signalEntity == null) {
            log.warn("Skipping signal {} because mapper returned null entity", signal.getSignalId());
            return;
        }

        List<SignalEntity> batchToPersist = List.of();
        synchronized (monitor) {
            pendingEntities.add(signalEntity);
            if (pendingEntities.size() >= batchSize) {
                batchToPersist = drainPendingUnsafe();
            }
        }

        persistBatch(batchToPersist);
    }

    void flushPending() {
        List<SignalEntity> batchToPersist;
        synchronized (monitor) {
            batchToPersist = drainPendingUnsafe();
        }

        persistBatch(batchToPersist);
    }

    @PreDestroy
    void flushBeforeShutdown() {
        flushPending();
    }

    private List<SignalEntity> drainPendingUnsafe() {
        if (pendingEntities.isEmpty()) {
            return List.of();
        }

        List<SignalEntity> drained = new ArrayList<>(pendingEntities);
        pendingEntities.clear();
        return drained;
    }

    private void persistBatch(List<SignalEntity> batchToPersist) {
        if (batchToPersist == null || batchToPersist.isEmpty()) {
            return;
        }

        springDataSignalRepository.saveAll(batchToPersist);
        log.info("Persisted {} signals from Kafka storage topic", batchToPersist.size());
    }
}

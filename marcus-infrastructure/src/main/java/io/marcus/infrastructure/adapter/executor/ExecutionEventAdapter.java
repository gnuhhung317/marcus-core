package io.marcus.infrastructure.adapter.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.executor.ExecutionEvent;
import io.marcus.domain.executor.ExecutionEventPort;
import io.marcus.domain.executor.ExecutionEventType;
import io.marcus.infrastructure.persistence.executor.ExecutionEventEntity;
import io.marcus.infrastructure.persistence.executor.ExecutionEventRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Infrastructure adapter for ExecutionEventPort.
 * Persists and retrieves execution events using Spring Data JPA.
 * Handles conversion between domain ExecutionEvent and JPA ExecutionEventEntity.
 */
@Component
public class ExecutionEventAdapter implements ExecutionEventPort {

    private final ExecutionEventRepository executionEventRepository;
    private final ObjectMapper objectMapper;

    public ExecutionEventAdapter(ExecutionEventRepository executionEventRepository) {
        this.executionEventRepository = executionEventRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ExecutionEvent store(ExecutionEvent event) {
        // Check for duplicate (idempotent)
        if (executionEventRepository.existsByEventId(event.getEventId())) {
            return event;
        }

        ExecutionEventEntity entity = new ExecutionEventEntity(
                event.getEventId(),
                event.getSignalId(),
                event.getSequence(),
                event.getEventType().getEventTypeCode(),
                event.getSentAt(),
                event.getExchangeTime(),
                event.getPayload(), // Object payload will be serialized in constructor
                event.getCreatedAt()
        );

        ExecutionEventEntity saved = executionEventRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public List<ExecutionEvent> findBySignalIdOrderBySequence(String signalId) {
        return executionEventRepository.findBySignalIdOrderBySequenceAsc(signalId)
                .stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionEvent> findBySignalIdAndSequenceRange(String signalId, int fromSequence, int toSequence) {
        return executionEventRepository.findBySignalIdAndSequenceRange(signalId, fromSequence, toSequence)
                .stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countBySignalId(String signalId) {
        return executionEventRepository.countBySignalId(signalId);
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return executionEventRepository.existsByEventId(eventId);
    }

    /**
     * Map JPA entity to domain object.
     */
    private ExecutionEvent mapToDomain(ExecutionEventEntity entity) {
        return new ExecutionEvent(
                entity.getEventId(),
                entity.getSignalId(),
                entity.getSequence(),
                ExecutionEventType.fromCode(entity.getEventType()),
                entity.getSentAt(),
                entity.getExchangeTime(),
                entity.getPayload(), // Returns JsonNode, which is generic Object
                entity.getCreatedAt()
        );
    }
}


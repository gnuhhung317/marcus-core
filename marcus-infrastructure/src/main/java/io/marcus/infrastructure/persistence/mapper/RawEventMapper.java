package io.marcus.infrastructure.persistence.mapper;

import io.marcus.domain.model.RawEvent;
import io.marcus.infrastructure.persistence.entity.RawEventEntity;
import org.springframework.stereotype.Component;

@Component
public class RawEventMapper {

    public RawEvent entityToDomain(RawEventEntity entity) {
        if (entity == null) {
            return null;
        }
        return RawEvent.builder()
            .id(entity.getId())
            .eventId(entity.getEventId())
            .botId(entity.getBotId())
            .idempotencyKey(entity.getIdempotencyKey())
            .correlationId(entity.getCorrelationId())
            .type(entity.getType())
            .payload(entity.getPayload())
            .receivedAt(entity.getReceivedAt())
            .sourceConnId(entity.getSourceConnId())
            .sequenceNo(entity.getSequenceNo())
            .processed(entity.getProcessed())
            .processedAt(entity.getProcessedAt())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .deletedAt(entity.getDeletedAt())
            .build();
    }

    public RawEventEntity domainToEntity(RawEvent domain) {
        if (domain == null) {
            return null;
        }
        return RawEventEntity.builder()
            .id(domain.getId())
            .eventId(domain.getEventId())
            .botId(domain.getBotId())
            .idempotencyKey(domain.getIdempotencyKey())
            .correlationId(domain.getCorrelationId())
            .type(domain.getType())
            .payload(domain.getPayload())
            .receivedAt(domain.getReceivedAt())
            .sourceConnId(domain.getSourceConnId())
            .sequenceNo(domain.getSequenceNo())
            .processed(domain.getProcessed())
            .processedAt(domain.getProcessedAt())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .deletedAt(domain.getDeletedAt())
            .build();
    }
}

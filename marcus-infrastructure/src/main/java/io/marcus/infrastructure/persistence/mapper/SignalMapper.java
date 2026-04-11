package io.marcus.infrastructure.persistence.mapper;

import io.marcus.domain.model.Signal;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import org.springframework.stereotype.Component;

@Component
public class SignalMapper {

    public Signal toDomain(SignalEntity entity) {
        if (entity == null) {
            return null;
        }
        return Signal.builder()
                .signalId(entity.getSignalId())
                .botId(entity.getBotId())
                .action(entity.getAction())
                .entry(entity.getEntry())
                .stopLoss(entity.getStopLoss())
                .takeProfit(entity.getTakeProfit())
                .status(entity.getStatus())
                .generatedTimestamp(entity.getGeneratedTimestamp())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public SignalEntity toEntity(Signal domain) {
        if (domain == null) {
            return null;
        }
        return SignalEntity.builder()
                .signalId(domain.getSignalId())
                .botId(domain.getBotId())
                .action(domain.getAction())
                .entry(domain.getEntry())
                .stopLoss(domain.getStopLoss())
                .takeProfit(domain.getTakeProfit())
                .status(domain.getStatus())
                .generatedTimestamp(domain.getGeneratedTimestamp())
                .metadata(domain.getMetadata())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}

package io.marcus.infrastructure.persistence.mapper;

import io.marcus.domain.model.Bot;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import org.springframework.stereotype.Component;

@Component
public class BotMapper {

    public Bot toDomain(BotEntity entity) {
        if (entity == null) return null;
        return Bot.builder()
                .botId(entity.getBotId())
                .name(entity.getName())
                .developerId(entity.getDeveloperId())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .tradingPair(entity.getTradingPair())
                .secretKey(entity.getSecretKey())
                .apiKey(entity.getApiKey())
                .exchangeId(entity.getExchange() != null ? entity.getExchange().getExchangeId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public BotEntity toEntity(Bot domain) {
        if (domain == null) return null;
        return BotEntity.builder()
                .botId(domain.getBotId())
                .name(domain.getName())
                .developerId(domain.getDeveloperId())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .tradingPair(domain.getTradingPair())
                .secretKey(domain.getSecretKey())
                .apiKey(domain.getApiKey())
                // exchange Mapping logic would go here if we were saving.
                // For now, mapping basic properties.
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}

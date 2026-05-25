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
                .symbol(entity.getSymbol())
                .action(entity.getAction())
                .marketType(entity.getMarketType())
                .orderType(entity.getOrderType())
                .entry(entity.getEntry())
                .stopLoss(entity.getStopLoss())
                .takeProfit(entity.getTakeProfit())
                .amount(entity.getAmount())
                .leverage(entity.getLeverage())
                .marginMode(entity.getMarginMode())
                .reduceOnly(entity.getReduceOnly())
                .status(entity.getStatus())
                .generatedTimestamp(entity.getGeneratedTimestamp())
                .timeframe(entity.getTimeframe())
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
                .symbol(domain.getSymbol())
                .action(domain.getAction())
                .marketType(domain.getMarketType())
                .orderType(domain.getOrderType())
                .entry(domain.getEntry())
                .stopLoss(domain.getStopLoss())
                .takeProfit(domain.getTakeProfit())
                .amount(domain.getAmount())
                .leverage(domain.getLeverage())
                .marginMode(domain.getMarginMode())
                .reduceOnly(domain.getReduceOnly())
                .status(domain.getStatus())
                .generatedTimestamp(domain.getGeneratedTimestamp())
                .timeframe(domain.getTimeframe())
                .metadata(domain.getMetadata())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}

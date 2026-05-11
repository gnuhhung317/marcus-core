package io.marcus.infrastructure.persistence.mapper;

import io.marcus.domain.model.UserPortfolio;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import org.springframework.stereotype.Component;

@Component
public class UserPortfolioMapper {

    public UserPortfolio toDomain(UserPortfolioEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserPortfolio.builder()
                .portfolioId(entity.getId())
                .userId(entity.getUserId())
                .totalCapital(entity.getTotalCapital())
                .availableBalance(entity.getAvailableBalance())
                .realizedPnl(entity.getRealizedPnl())
                .unrealizedPnl(entity.getUnrealizedPnl())
                .maxDrawdownThreshold(entity.getMaxDrawdownThreshold())
                .mediumRiskThreshold(entity.getMediumRiskThreshold())
                .exchangeId(entity.getExchangeId())
                .lastSyncAt(entity.getLastSyncAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public UserPortfolioEntity toEntity(UserPortfolio domain) {
        if (domain == null) {
            return null;
        }

        return UserPortfolioEntity.builder()
                .id(domain.getPortfolioId())
                .userId(domain.getUserId())
                .totalCapital(domain.getTotalCapital())
                .availableBalance(domain.getAvailableBalance())
                .realizedPnl(domain.getRealizedPnl())
                .unrealizedPnl(domain.getUnrealizedPnl())
                .maxDrawdownThreshold(domain.getMaxDrawdownThreshold())
                .mediumRiskThreshold(domain.getMediumRiskThreshold())
                .exchangeId(domain.getExchangeId())
                .lastSyncAt(domain.getLastSyncAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}

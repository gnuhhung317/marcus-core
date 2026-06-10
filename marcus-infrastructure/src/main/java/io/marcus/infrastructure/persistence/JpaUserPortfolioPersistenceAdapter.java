package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.UserPortfolio;
import io.marcus.domain.port.UserPortfolioPersistencePort;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import io.marcus.infrastructure.persistence.entity.PortfolioBalanceHistoryEntity;
import io.marcus.infrastructure.persistence.mapper.UserPortfolioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaUserPortfolioPersistenceAdapter implements UserPortfolioPersistencePort {

    private final SpringDataUserPortfolioRepository springDataUserPortfolioRepository;
    private final SpringDataPortfolioHistoryRepository springDataPortfolioHistoryRepository;
    private final UserPortfolioMapper userPortfolioMapper;

    @Override
    public UserPortfolio save(UserPortfolio userPortfolio) {
        UserPortfolioEntity entity = userPortfolioMapper.toEntity(userPortfolio);
        springDataUserPortfolioRepository.findByUserId(userPortfolio.getUserId())
                .ifPresent(existing -> entity.setId(existing.getId()));
        UserPortfolioEntity savedEntity = springDataUserPortfolioRepository.save(entity);
        return userPortfolioMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<UserPortfolio> findByUserId(String userId) {
        return springDataUserPortfolioRepository.findByUserId(userId)
                .map(userPortfolioMapper::toDomain);
    }

    @Override
    public void saveHistory(String userId, java.math.BigDecimal total, java.math.BigDecimal free, java.math.BigDecimal used, java.math.BigDecimal unrealizedPnl, String exchangeId) {
        PortfolioBalanceHistoryEntity entity = PortfolioBalanceHistoryEntity.builder()
                .userId(userId)
                .total(total)
                .free(free)
                .used(used)
                .unrealizedPnl(unrealizedPnl)
                .exchangeId(exchangeId)
                .snapshotAt(LocalDateTime.now())
                .build();
        springDataPortfolioHistoryRepository.save(entity);
    }
}

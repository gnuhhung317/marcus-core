package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.UserPortfolio;
import io.marcus.domain.port.UserPortfolioPersistencePort;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import io.marcus.infrastructure.persistence.mapper.UserPortfolioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaUserPortfolioPersistenceAdapter implements UserPortfolioPersistencePort {

    private final SpringDataUserPortfolioRepository springDataUserPortfolioRepository;
    private final UserPortfolioMapper userPortfolioMapper;

    @Override
    public UserPortfolio save(UserPortfolio userPortfolio) {
        UserPortfolioEntity entity = userPortfolioMapper.toEntity(userPortfolio);
        UserPortfolioEntity savedEntity = springDataUserPortfolioRepository.save(entity);
        return userPortfolioMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<UserPortfolio> findByUserId(String userId) {
        return springDataUserPortfolioRepository.findByUserId(userId)
                .map(userPortfolioMapper::toDomain);
    }
}

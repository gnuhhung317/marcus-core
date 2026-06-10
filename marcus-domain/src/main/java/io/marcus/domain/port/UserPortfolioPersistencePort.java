package io.marcus.domain.port;

import io.marcus.domain.model.UserPortfolio;

import java.util.Optional;

public interface UserPortfolioPersistencePort {

    UserPortfolio save(UserPortfolio userPortfolio);

    Optional<UserPortfolio> findByUserId(String userId);

    void saveHistory(String userId, java.math.BigDecimal total, java.math.BigDecimal free, java.math.BigDecimal used, java.math.BigDecimal unrealizedPnl, String exchangeId);
}

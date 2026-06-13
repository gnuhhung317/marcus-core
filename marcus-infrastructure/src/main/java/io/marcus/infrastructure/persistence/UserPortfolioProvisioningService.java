package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.UserEntity;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserPortfolioProvisioningService {

    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataUserPortfolioRepository springDataUserPortfolioRepository;

    @Transactional
    public void ensurePortfolioExists(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        springDataUserPortfolioRepository.findByUserId(userId)
                .orElseGet(() -> springDataUserPortfolioRepository.save(defaultPortfolio(userId)));
    }

    @Transactional
    public int backfillMissingPortfolios() {
        Set<String> existingPortfolioUserIds = new HashSet<>();
        springDataUserPortfolioRepository.findAll().stream()
                .map(UserPortfolioEntity::getUserId)
                .filter(value -> value != null && !value.isBlank())
                .forEach(existingPortfolioUserIds::add);

        int created = 0;
        for (UserEntity user : springDataUserRepository.findAll()) {
            String userId = user.getUserId();
            if (userId == null || userId.isBlank() || existingPortfolioUserIds.contains(userId)) {
                continue;
            }
            springDataUserPortfolioRepository.save(defaultPortfolio(userId));
            existingPortfolioUserIds.add(userId);
            created++;
        }
        return created;
    }

    private UserPortfolioEntity defaultPortfolio(String userId) {
        return UserPortfolioEntity.builder()
                .userId(userId)
                .totalCapital(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .realizedPnl(BigDecimal.ZERO)
                .unrealizedPnl(BigDecimal.ZERO)
                .maxDrawdownThreshold(new BigDecimal("0.1000"))
                .mediumRiskThreshold(new BigDecimal("0.0500"))
                .freshAccountsCount(0)
                .staleAccountsCount(0)
                .dataFreshness("STALE")
                .build();
    }
}

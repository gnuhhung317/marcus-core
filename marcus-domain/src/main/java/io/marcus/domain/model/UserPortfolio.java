package io.marcus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserPortfolio extends BaseModel {

    private String portfolioId;
    private String userId;
    
    private BigDecimal totalCapital;
    private BigDecimal availableBalance;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;

    private BigDecimal maxDrawdownThreshold;
    private BigDecimal mediumRiskThreshold;
    
    private String exchangeId;
    private LocalDateTime lastSyncAt;

    public static UserPortfolio createDefault(String userId) {
        return UserPortfolio.builder()
                .userId(userId)
                .totalCapital(new BigDecimal("10000"))
                .availableBalance(new BigDecimal("10000"))
                .realizedPnl(BigDecimal.ZERO)
                .unrealizedPnl(BigDecimal.ZERO)
                .maxDrawdownThreshold(new BigDecimal("0.1000")) // 10%
                .mediumRiskThreshold(new BigDecimal("0.0500"))  // 5%
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void updateBalance(BigDecimal total, BigDecimal freeBalance, BigDecimal unrealizedPnL, String exchange) {
        this.totalCapital = total;
        this.availableBalance = freeBalance;
        this.unrealizedPnl = unrealizedPnL;
        this.exchangeId = exchange;
        this.lastSyncAt = LocalDateTime.now();
        this.setUpdatedAt(LocalDateTime.now());
    }
}

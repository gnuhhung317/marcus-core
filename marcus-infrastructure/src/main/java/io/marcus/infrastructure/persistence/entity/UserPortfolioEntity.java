package io.marcus.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_portfolios",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_user_portfolios_user_id", columnNames = {"user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserPortfolioEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "total_capital", precision = 18, scale = 8, nullable = false)
    private BigDecimal totalCapital;

    @Column(name = "available_balance", precision = 18, scale = 8)
    private BigDecimal availableBalance;

    @Column(name = "realized_pnl", precision = 18, scale = 8)
    private BigDecimal realizedPnl;

    @Column(name = "unrealized_pnl", precision = 18, scale = 8)
    private BigDecimal unrealizedPnl;

    @Column(name = "max_drawdown_threshold", precision = 5, scale = 4, nullable = false)
    private BigDecimal maxDrawdownThreshold;

    @Column(name = "medium_risk_threshold", precision = 5, scale = 4, nullable = false)
    private BigDecimal mediumRiskThreshold;

    @Column(name = "exchange_id")
    private String exchangeId;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "fresh_accounts_count", nullable = false)
    private Integer freshAccountsCount = 0;

    @Column(name = "stale_accounts_count", nullable = false)
    private Integer staleAccountsCount = 0;

    @Column(name = "data_freshness", nullable = false)
    private String dataFreshness = "STALE";
}

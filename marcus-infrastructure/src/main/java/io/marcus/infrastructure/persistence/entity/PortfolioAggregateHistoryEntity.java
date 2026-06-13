package io.marcus.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_aggregate_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PortfolioAggregateHistoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "total", precision = 18, scale = 8, nullable = false)
    private BigDecimal total;

    @Column(name = "free", precision = 18, scale = 8, nullable = false)
    private BigDecimal free;

    @Column(name = "used", precision = 18, scale = 8, nullable = false)
    private BigDecimal used;

    @Column(name = "realized_pnl", precision = 18, scale = 8, nullable = false)
    private BigDecimal realizedPnl;

    @Column(name = "unrealized_pnl", precision = 18, scale = 8, nullable = false)
    private BigDecimal unrealizedPnl;

    @Column(name = "fresh_accounts_count", nullable = false)
    private Integer freshAccountsCount;

    @Column(name = "stale_accounts_count", nullable = false)
    private Integer staleAccountsCount;

    @Column(name = "data_freshness", nullable = false)
    private String dataFreshness;

    @Column(name = "exchange_id")
    private String exchangeId;

    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;
}

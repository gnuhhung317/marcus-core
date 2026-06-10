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
@Table(name = "portfolio_balance_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PortfolioBalanceHistoryEntity extends BaseEntity {

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

    @Column(name = "unrealized_pnl", precision = 18, scale = 8, nullable = false)
    private BigDecimal unrealizedPnl;

    @Column(name = "exchange_id")
    private String exchangeId;

    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;
}

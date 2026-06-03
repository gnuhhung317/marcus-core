package io.marcus.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bot_historical_portfolios",
        indexes = {
                @Index(name = "idx_bot_historical_portfolio_run_time", columnList = "run_id,timestamp", unique = true),
                @Index(name = "idx_bot_historical_portfolio_bot_time", columnList = "bot_id,timestamp")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BotHistoricalPortfolioEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "bot_id", nullable = false)
    private String botId;

    @Column(name = "data_source", nullable = false)
    private String dataSource;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal cash;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal equity;

    @Column(name = "realized_pnl", nullable = false, precision = 18, scale = 8)
    private BigDecimal realizedPnl;

    @Column(name = "unrealized_pnl", nullable = false, precision = 18, scale = 8)
    private BigDecimal unrealizedPnl;

    @Column(name = "total_fees", nullable = false, precision = 18, scale = 8)
    private BigDecimal totalFees;
}

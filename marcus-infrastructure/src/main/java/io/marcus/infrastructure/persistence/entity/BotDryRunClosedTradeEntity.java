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
        name = "bot_dry_run_closed_trades",
        indexes = {
                @Index(name = "idx_bot_dry_run_trade_bot_trade", columnList = "bot_id,trade_id", unique = true),
                @Index(name = "idx_bot_dry_run_trade_bot_exit", columnList = "bot_id,exit_timestamp")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BotDryRunClosedTradeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "bot_id", nullable = false)
    private String botId;

    @Column(name = "trade_id", nullable = false)
    private String tradeId;

    @Column(name = "data_source", nullable = false)
    private String dataSource;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "market_type", nullable = false)
    private String marketType;

    @Column(nullable = false)
    private String side;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(name = "entry_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal entryPrice;

    @Column(name = "exit_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal exitPrice;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal pnl;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal fees;

    @Column(name = "entry_timestamp", nullable = false)
    private LocalDateTime entryTimestamp;

    @Column(name = "exit_timestamp", nullable = false)
    private LocalDateTime exitTimestamp;

    @Column(name = "entry_signal_id")
    private String entrySignalId;

    @Column(name = "exit_signal_id")
    private String exitSignalId;
}

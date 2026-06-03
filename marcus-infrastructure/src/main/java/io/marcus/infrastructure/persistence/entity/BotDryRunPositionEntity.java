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
        name = "bot_dry_run_positions",
        indexes = {
                @Index(name = "idx_bot_dry_run_position_bot_position", columnList = "bot_id,position_id", unique = true),
                @Index(name = "idx_bot_dry_run_position_bot_status", columnList = "bot_id,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BotDryRunPositionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "bot_id", nullable = false)
    private String botId;

    @Column(name = "position_id", nullable = false)
    private String positionId;

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

    @Column(name = "current_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal currentPrice;

    @Column(name = "unrealized_pnl", nullable = false, precision = 18, scale = 8)
    private BigDecimal unrealizedPnl;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "source_signal_id")
    private String sourceSignalId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}

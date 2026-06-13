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
        name = "portfolio_accounts",
        indexes = {
                @Index(name = "idx_portfolio_accounts_user_sync", columnList = "user_id,last_sync_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PortfolioAccountEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_subscription_id", nullable = false, unique = true)
    private String userSubscriptionId;

    @Column(name = "bot_id", nullable = false)
    private String botId;

    @Column(name = "ws_token", nullable = false)
    private String wsToken;

    @Column(name = "exchange_id")
    private String exchangeId;

    @Column(name = "currency")
    private String currency;

    @Column(name = "execution_mode")
    private String executionMode;

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

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}

package io.marcus.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA entity for storing pre-calculated leaderboard metrics. Uses composite
 * primary key (bot_id, data_source) to allow one bot to have both DRY_RUN and
 * HISTORICAL metrics.
 */
@Entity
@Table(name = "bot_leaderboard_metrics")
@IdClass(BotLeaderboardMetricsEntity.BotLeaderboardMetricsId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotLeaderboardMetricsEntity {

    @Id
    @Column(name = "bot_id", nullable = false)
    private String botId;

    @Id
    @Column(name = "data_source", nullable = false)
    private String dataSource;  // "DRY_RUN" or "HISTORICAL"

    @Column(name = "cagr", nullable = false)
    private double cagr;

    @Column(name = "sharpe", nullable = false)
    private double sharpe;

    @Column(name = "max_drawdown", nullable = false)
    private double maxDrawdown;

    @Column(name = "sample_days", nullable = false)
    private long sampleDays;

    @Column(name = "last_calculated_at", nullable = false)
    private LocalDateTime lastCalculatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastCalculatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastCalculatedAt = LocalDateTime.now();
    }

    /**
     * Composite primary key class for bot_leaderboard_metrics table. Allows one
     * bot to have separate metrics for DRY_RUN and HISTORICAL data sources.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BotLeaderboardMetricsId implements Serializable {

        private String botId;
        private String dataSource;
    }
}

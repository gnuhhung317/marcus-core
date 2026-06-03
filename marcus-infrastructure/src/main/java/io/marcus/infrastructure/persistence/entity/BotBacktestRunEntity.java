package io.marcus.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "bot_backtest_runs",
        indexes = {
                @Index(name = "idx_bot_backtest_run_bot_created", columnList = "bot_id,created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BotBacktestRunEntity extends BaseEntity {

    @Id
    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "bot_id", nullable = false)
    private String botId;

    @Column(name = "run_name")
    private String runName;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;
}

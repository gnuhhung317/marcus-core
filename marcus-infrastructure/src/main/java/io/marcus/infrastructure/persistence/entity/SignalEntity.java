package io.marcus.infrastructure.persistence.entity;

import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "signals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SignalEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String signalId;

    @Column(nullable = false)
    private String botId;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignalAction action;

    @Column(precision = 18, scale = 8)
    private BigDecimal entry;

    @Column(name = "stop_loss", precision = 18, scale = 8)
    private BigDecimal stopLoss;

    @Column(name = "take_profit", precision = 18, scale = 8)
    private BigDecimal takeProfit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignalStatus status;

    private LocalDateTime generatedTimestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}

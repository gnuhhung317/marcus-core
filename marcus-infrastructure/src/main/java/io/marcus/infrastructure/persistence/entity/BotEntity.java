package io.marcus.infrastructure.persistence.entity;

import io.marcus.domain.vo.BotStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "bots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BotEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String botId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String developerId;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotStatus status;

    private String tradingPair;

    @Column(name = "secret_key")
    private String secretKey;

    @Column(name = "api_key")
    private String apiKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id")
    private ExchangeEntity exchange;
}

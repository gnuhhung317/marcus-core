package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.BotTelemetryPoint;
import io.marcus.domain.port.BotTelemetryPort;
import io.marcus.infrastructure.persistence.entity.BotTelemetryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaBotTelemetryAdapter implements BotTelemetryPort {

    private final SpringDataBotTelemetryRepository repository;

    @Override
    @Transactional
    public BotTelemetryPoint save(BotTelemetryPoint point) {
        BotTelemetryEntity entity = repository.findByBotIdAndTimestamp(point.botId(), point.timestamp())
                .orElseGet(BotTelemetryEntity::new);
        entity.setBotId(point.botId());
        entity.setTimestamp(point.timestamp());
        entity.setEquity(point.equity());
        entity.setRealizedPnl(point.realizedPnl());
        entity.setUnrealizedPnl(point.unrealizedPnl());
        entity.setMetricsJson(point.metricsJson());
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BotTelemetryPoint> findLatest(String botId) {
        return repository.findTopByBotIdOrderByTimestampDesc(botId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotTelemetryPoint> findByBotIdSince(String botId, LocalDateTime since) {
        return repository.findByBotIdAndTimestampGreaterThanEqualOrderByTimestampAsc(botId, since).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotTelemetryPoint> findByBotId(String botId) {
        return repository.findByBotIdOrderByTimestampAsc(botId).stream()
                .map(this::toDomain)
                .toList();
    }

    private BotTelemetryPoint toDomain(BotTelemetryEntity entity) {
        return new BotTelemetryPoint(
                entity.getBotId(),
                entity.getTimestamp(),
                entity.getEquity(),
                entity.getRealizedPnl(),
                entity.getUnrealizedPnl(),
                entity.getMetricsJson()
        );
    }
}

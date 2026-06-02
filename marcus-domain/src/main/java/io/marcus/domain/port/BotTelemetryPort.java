package io.marcus.domain.port;

import io.marcus.domain.model.BotTelemetryPoint;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BotTelemetryPort {

    BotTelemetryPoint save(BotTelemetryPoint point);

    Optional<BotTelemetryPoint> findLatest(String botId);

    List<BotTelemetryPoint> findByBotIdSince(String botId, LocalDateTime since);

    List<BotTelemetryPoint> findByBotId(String botId);
}

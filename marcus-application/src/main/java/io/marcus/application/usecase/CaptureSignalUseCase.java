package io.marcus.application.usecase;

import io.marcus.application.dto.CaptureSignalRequest;
import io.marcus.application.dto.ResolveBotRoutingTargetsRequest;
import io.marcus.domain.model.Signal;
import io.marcus.domain.vo.SignalStatus;
import io.marcus.domain.port.SignalPublisherPort;
import io.marcus.domain.port.SignalServerDispatchPort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CaptureSignalUseCase {

    private final SignalRepository signalRepository;
    private final BotRepository botRepository;
    private final ResolveBotRoutingTargetsUseCase resolveBotRoutingTargetsUseCase;
    private final SignalPublisherPort signalPublisherPort;
    private final SignalServerDispatchPort signalServerDispatchPort;

    public void execute(CaptureSignalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Signal request is required");
        }

        // Validate that the bot exists in the database
        if (!botRepository.findByBotId(request.botId()).isPresent()) {
            throw new IllegalArgumentException("Bot not found: " + request.botId());
        }

        // Check for duplicate signal ID
        if (signalRepository.existsBySignalId(request.signalId())) {
            throw new IllegalArgumentException("Signal already exists: " + request.signalId());
        }

        Signal signal = Signal.builder()
                .signalId(request.signalId())
                .botId(request.botId())
                .symbol(request.symbol())
                .action(request.action())
                .entry(request.entry())
                .stopLoss(request.stopLoss())
                .takeProfit(request.takeProfit())
                .status(request.status() != null ? request.status() : SignalStatus.RECEIVED)
                .generatedTimestamp(request.generatedTimestamp() != null ? request.generatedTimestamp() : LocalDateTime.now())
                .timeframe(request.timeframe())
                .metadata(request.metadata())
                .build();

        // 1. Persist signal to PostgreSQL first
        signalRepository.save(signal);

        // 2. Publish signal to Kafka for message queue
        signalPublisherPort.publish(signal);

        Set<String> targetServerIds = resolveBotRoutingTargetsUseCase
                .execute(new ResolveBotRoutingTargetsRequest(signal.getBotId()));
        if (targetServerIds.isEmpty()) {
            return;
        }

        // 3. Dispatch signal to target servers for real-time routing
        signalServerDispatchPort.dispatchToServers(signal, targetServerIds);
    }
}

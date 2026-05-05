package io.marcus.application.usecase;

import io.marcus.application.dto.ResolveBotRoutingTargetsRequest;
import io.marcus.domain.model.Signal;
import io.marcus.domain.port.SignalPublisherPort;
import io.marcus.domain.port.SignalServerDispatchPort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CaptureSignalUseCase {
    private final SignalRepository signalRepository;
    private final BotRepository botRepository;
    private final ResolveBotRoutingTargetsUseCase resolveBotRoutingTargetsUseCase;
    private final SignalPublisherPort signalPublisherPort;
    private final SignalServerDispatchPort signalServerDispatchPort;

    public void execute(Signal signal){
        if (signal == null) {
            throw new IllegalArgumentException("Signal is required");
        }
        if (signal.getBotId() == null || signal.getBotId().isBlank()) {
            throw new IllegalArgumentException("Signal bot id is required");
        }
        if (signal.getSignalId() == null || signal.getSignalId().isBlank()) {
            throw new IllegalArgumentException("Signal id is required");
        }
        
        // Validate that the bot exists in the database
        if (!botRepository.findByBotId(signal.getBotId()).isPresent()) {
            throw new IllegalArgumentException("Bot not found: " + signal.getBotId());
        }
        
        // Check for duplicate signal ID
        if (signalRepository.existsBySignalId(signal.getSignalId())) {
            throw new IllegalArgumentException("Signal already exists: " + signal.getSignalId());
        }

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

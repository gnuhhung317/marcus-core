package io.marcus.application.usecase;

import io.marcus.application.dto.ResolveBotRoutingTargetsRequest;
import io.marcus.domain.model.Signal;
import io.marcus.domain.port.SignalServerDispatchPort;
import io.marcus.domain.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CaptureSignalUseCase {
    private final SignalRepository signalRepository;
    private final ResolveBotRoutingTargetsUseCase resolveBotRoutingTargetsUseCase;
    private final SignalServerDispatchPort signalServerDispatchPort;

    public void execute(Signal signal){
        if (signal == null) {
            throw new IllegalArgumentException("Signal is required");
        }
        if (signal.getBotId() == null || signal.getBotId().isBlank()) {
            throw new IllegalArgumentException("Signal bot id is required");
        }

        signalRepository.publish(signal);

        Set<String> targetServerIds = resolveBotRoutingTargetsUseCase
                .execute(new ResolveBotRoutingTargetsRequest(signal.getBotId()));
        if (targetServerIds.isEmpty()) {
            return;
        }

        signalServerDispatchPort.dispatchToServers(signal, targetServerIds);
    }

}

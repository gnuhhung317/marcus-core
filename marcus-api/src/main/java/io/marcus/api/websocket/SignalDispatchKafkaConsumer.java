package io.marcus.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.Signal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SignalDispatchKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final ExecutorSessionRegistry sessionRegistry;

    @KafkaListener(topics = "trading-signals", groupId = "marcus-websocket-dispatcher")
    public void consume(String signalJson) {
        try {
            Signal signal = objectMapper.readValue(signalJson, Signal.class);
            if (signal.getSymbol() == null || signal.getSymbol().isBlank()) {
                log.warn("Skipping signal {} because symbol is missing", signal.getSignalId());
                return;
            }

            dispatch(signal);
        } catch (Exception exception) {
            log.warn("Failed to dispatch kafka signal payload: {}", exception.getMessage());
        }
    }

    private void dispatch(Signal signal) {
        String frame = buildSignalFrame(signal);
        sessionRegistry.broadcastToBot(signal.getBotId(), frame);
    }

    private String buildSignalFrame(Signal signal) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("signal_id", signal.getSignalId());
            payload.put("bot_id", signal.getBotId());
            payload.put("symbol", signal.getSymbol());
            payload.put("action", signal.getAction() != null ? signal.getAction().name() : null);
            payload.put("entry", signal.getEntry());
            payload.put("stop_loss", signal.getStopLoss());
            payload.put("take_profit", signal.getTakeProfit());
            payload.put("status", signal.getStatus() != null ? signal.getStatus().name() : null);
            payload.put("generated_timestamp", signal.getGeneratedTimestamp() != null ? signal.getGeneratedTimestamp().toString() : null);
            payload.put("metadata", signal.getMetadata());

            Map<String, Object> frame = new HashMap<>();
            frame.put("type", "signal");
            frame.put("payload", payload);
            return objectMapper.writeValueAsString(frame);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build signal websocket frame", exception);
        }
    }
}

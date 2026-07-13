package io.marcus.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.Signal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that delivers incoming signals to connected WebSocket executor
 * clients.
 *
 * <p>
 * Responsibilities (single):
 * <ol>
 * <li>Deserialize the Kafka message into a {@link Signal} domain object.</li>
 * <li>Delegate frame construction to {@link SignalFrameBuilder}.</li>
 * <li>Broadcast the serialized frame to all sessions registered for the
 * bot.</li>
 * </ol>
 *
 * <p>
 * Frame building and expiry calculation logic live in
 * {@link SignalFrameBuilder}
 * — this class stays intentionally thin.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SignalDispatchKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final ExecutorSessionRegistry sessionRegistry;
    private final SignalFrameBuilder signalFrameBuilder;

    @KafkaListener(topics = "${marcus.messaging.signal-storage-topic:trading-signals}", groupId = "#{'marcus-websocket-dispatcher-' + T(java.util.UUID).randomUUID().toString()}", properties = "auto.offset.reset=latest")
    public void consume(String signalJson) {
        try {
            Signal signal = objectMapper.readValue(signalJson, Signal.class);

            if (signal.getSymbol() == null || signal.getSymbol().isBlank()) {
                log.warn("[Dispatch] Skipping signal with missing symbol signalId={}",
                        signal.getSignalId());
                return;
            }

            String frame = objectMapper.writeValueAsString(signalFrameBuilder.buildFrame(signal));
            sessionRegistry.broadcastToBot(signal.getBotId(), frame);

            log.debug("[Dispatch] Broadcasted signalId={} botId={} action={} marketType={}",
                    signal.getSignalId(), signal.getBotId(),
                    signal.getAction(), signal.getMarketType());

        } catch (Exception ex) {
            log.warn("[Dispatch] Failed to process Kafka signal payload: {}", ex.getMessage());
        }
    }
}

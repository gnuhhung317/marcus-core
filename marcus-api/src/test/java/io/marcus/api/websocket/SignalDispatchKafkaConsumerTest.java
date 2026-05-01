package io.marcus.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.Signal;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SignalDispatchKafkaConsumerTest {

    @Test
    void shouldConvertKafkaSignalIntoWebSocketFrameWithSignalSymbol() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ExecutorSessionRegistry sessionRegistry = mock(ExecutorSessionRegistry.class);

        SignalDispatchKafkaConsumer consumer = new SignalDispatchKafkaConsumer(objectMapper, sessionRegistry);

        Signal signal = new Signal();
        signal.setSignalId("sig-1");
        signal.setBotId("bot-1");
        signal.setSymbol("BTC/USDT");
        signal.setAction(SignalAction.OPEN_LONG);
        signal.setEntry(new BigDecimal("123.45"));
        signal.setStopLoss(new BigDecimal("120.00"));
        signal.setTakeProfit(new BigDecimal("130.00"));
        signal.setStatus(SignalStatus.RECEIVED);
        signal.setGeneratedTimestamp(LocalDateTime.parse("2026-05-01T00:00:00"));
        signal.setMetadata(java.util.Map.of("strategy", "sma"));

        consumer.consume(signal);

        org.mockito.ArgumentCaptor<String> frameCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sessionRegistry).broadcastToBot(eq("bot-1"), frameCaptor.capture());
        assertTrue(frameCaptor.getValue().contains("\"type\":\"signal\""));
        assertTrue(frameCaptor.getValue().contains("\"signal_id\":\"sig-1\""));
        assertTrue(frameCaptor.getValue().contains("\"symbol\":\"BTC/USDT\""));
        assertTrue(frameCaptor.getValue().contains("\"action\":\"OPEN_LONG\""));
    }
}

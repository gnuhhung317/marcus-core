package io.marcus.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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

        consumer.consume("""
            {"signalId":"sig-1","botId":"bot-1","symbol":"BTC/USDT","action":"OPEN_LONG","entry":123.45,"stopLoss":120.00,"takeProfit":130.00,"status":"RECEIVED","generatedTimestamp":"2026-05-01T00:00:00","metadata":{"strategy":"sma"}}
            """);

        org.mockito.ArgumentCaptor<String> frameCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sessionRegistry).broadcastToBot(eq("bot-1"), frameCaptor.capture());
        assertTrue(frameCaptor.getValue().contains("\"type\":\"signal\""));
        assertTrue(frameCaptor.getValue().contains("\"signal_id\":\"sig-1\""));
        assertTrue(frameCaptor.getValue().contains("\"symbol\":\"BTC/USDT\""));
        assertTrue(frameCaptor.getValue().contains("\"action\":\"OPEN_LONG\""));
    }
}

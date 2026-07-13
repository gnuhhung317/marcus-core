package io.marcus.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.application.dto.CaptureSignalRequest;
import io.marcus.domain.model.RawEvent;
import io.marcus.domain.vo.MarketType;
import io.marcus.domain.vo.OrderType;
import io.marcus.domain.vo.SignalAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessRawEventUseCaseTest {

    @Mock
    private CaptureSignalUseCase captureSignalUseCase;

    @Test
    void shouldPreservePoliciesWhenRebuildingRequestFromRawEvent() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        ProcessRawEventUseCase processRawEventUseCase = new ProcessRawEventUseCase(
                captureSignalUseCase,
                objectMapper
        );
        Map<String, Object> policies = Map.of(
                "maxSizePercent", 0.2,
                "cancelOrderAfter", 1_800
        );
        RawEvent rawEvent = RawEvent.builder()
                .eventId("raw-event-1")
                .botId("bot-from-event")
                .type("ingest")
                .payload(Map.of(
                        "symbol", "BTCUSDT",
                        "action", "OPEN_LONG",
                        "marketType", "SPOT",
                        "orderType", "MARKET",
                        "generatedTimestamp", "2026-05-24T10:15:30",
                        "metadata", Map.of("source", "raw-event"),
                        "policies", policies
                ))
                .build();

        processRawEventUseCase.execute(rawEvent);

        ArgumentCaptor<CaptureSignalRequest> requestCaptor = ArgumentCaptor.forClass(CaptureSignalRequest.class);
        verify(captureSignalUseCase).execute(requestCaptor.capture());

        CaptureSignalRequest request = requestCaptor.getValue();
        assertEquals("raw-event-1", request.signalId());
        assertEquals("bot-from-event", request.botId());
        assertEquals("BTCUSDT", request.symbol());
        assertEquals(SignalAction.OPEN_LONG, request.action());
        assertEquals(MarketType.SPOT, request.marketType());
        assertEquals(OrderType.MARKET, request.orderType());
        assertEquals(LocalDateTime.of(2026, 5, 24, 10, 15, 30), request.generatedTimestamp());
        assertEquals(policies, request.policies());
    }
}

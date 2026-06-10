package io.marcus.api.websocket.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.websocket.ExecutorHandshakeInterceptor;
import io.marcus.application.dto.BalanceSyncRequest;
import io.marcus.application.usecase.BalanceSyncUseCase;
import io.marcus.domain.model.RawEvent;
import io.marcus.domain.port.RawEventPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Specialized Handler for 'audit-push' frame type.
 * Persists general event frames into RawEvent table and routes 'balance_snapshot' kind
 * to BalanceSyncUseCase.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditPushEventHandler {

    private final BalanceSyncUseCase balanceSyncUseCase;
    private final RawEventPersistencePort rawEventPersistencePort;
    private final ObjectMapper objectMapper;

    public void handleAuditPush(WebSocketSession session, JsonNode frameRoot) {
        try {
            JsonNode payloadNode = frameRoot.path("payload");
            String kind = payloadNode.path("kind").asText("unknown");
            String botId = (String) session.getAttributes().get("botId");
            String userId = (String) session.getAttributes().get(ExecutorHandshakeInterceptor.USER_ID_ATTRIBUTE);

            // Map the raw json to Map for RawEvent storage
            Map<String, Object> rawPayload = objectMapper.convertValue(payloadNode, new TypeReference<Map<String, Object>>() {});

            // 1. Persist audit trail
            RawEvent auditTrail = RawEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .botId(botId != null ? botId : "system")
                    .type("audit-push")
                    .payload(rawPayload)
                    .idempotencyKey(payloadNode.path("timestamp").asText(String.valueOf(Instant.now().toEpochMilli())))
                    .correlationId(kind)
                    .sourceConnId(session.getId())
                    .receivedAt(Instant.now())
                    .processed(true)
                    .processedAt(Instant.now())
                    .build();

            rawEventPersistencePort.save(auditTrail);

            // 2. Route if kind is balance_snapshot or balance-snapshot
            if ("balance_snapshot".equals(kind) || "balance-snapshot".equals(kind)) {
                processBalanceSnapshot(userId, payloadNode);
            } else {
                log.debug("Received unhandled audit-push kind: {}", kind);
            }

        } catch (Exception e) {
            log.error("CRITICAL: Failed processing audit-push frame from websocket", e);
        }
    }

    private void processBalanceSnapshot(String userId, JsonNode node) {
        if (userId == null) {
            log.warn("Skipping balance snapshot processing: session userId is null");
            return;
        }

        try {
            BigDecimal total = getBigDecimalSafely(node, "total", BigDecimal.ZERO);
            BigDecimal free = getBigDecimalSafely(node, "free", BigDecimal.ZERO);
            BigDecimal used = getBigDecimalSafely(node, "used", BigDecimal.ZERO);
            BigDecimal unrealized = getBigDecimalSafely(node, "unrealizedPnl", BigDecimal.ZERO);
            String exchange = node.path("exchange").asText("unknown");

            BalanceSyncRequest request = new BalanceSyncRequest(
                    total,
                    free,
                    used,
                    unrealized,
                    exchange
            );

            balanceSyncUseCase.execute(userId, request);
        } catch (Exception ex) {
            log.error("Error parsing balance snapshot data", ex);
        }
    }

    private BigDecimal getBigDecimalSafely(JsonNode node, String field, BigDecimal defaultValue) {
        JsonNode val = node.path(field);
        if (val.isMissingNode() || val.isNull()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(val.asText());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

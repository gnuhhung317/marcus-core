package io.marcus.api.websocket;

import io.marcus.domain.model.Signal;
import io.marcus.domain.vo.MarginMode;
import io.marcus.domain.vo.MarketType;
import io.marcus.domain.vo.OrderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the WebSocket signal frame sent to executor clients.
 *
 * <p>Centralises all transport-layer serialization logic so that the
 * {@link SignalDispatchKafkaConsumer} (and any future dispatch paths) stay free
 * of frame-construction concerns.
 *
 * <p>Frame structure (JSON):
 * <pre>{@code
 * {
 *   "type": "signal",
 *   "payload": {
 *     "signal_id":             string,
 *     "bot_id":                string,
 *     "symbol":                string,
 *     "action":                "OPEN_LONG" | "CLOSE_LONG" | "OPEN_SHORT" | "CLOSE_SHORT" | "UPDATE_TP_SL",
 *     "market_type":           "SPOT" | "FUTURE" | "MARGIN",
 *     "order_type":            "LIMIT" | "MARKET",
 *     "entry":                 decimal | null,
 *     "stop_loss":             decimal | null,
 *     "take_profit":           decimal | null,
 *     "amount":                decimal | null,
 *     "leverage":              int,
 *     "margin_mode":           "CROSS" | "ISOLATED",
 *     "reduce_only":           boolean | null,
 *     "status":                string,
 *     "timeframe":             string | null,
 *     "cancel_after_timestamp": epoch-seconds (long),
 *     "generated_timestamp":   ISO-8601 | null,
 *     "metadata":              object | null
 *   }
 * }
 * }</pre>
 */
@Component
@Slf4j
public class SignalFrameBuilder {

    /** Default cancel window when timeframe is unknown: 1 hour. */
    private static final long DEFAULT_EXPIRY_SECONDS = 3_600L;

    /**
     * Build the full WebSocket frame map for the given signal.
     *
     * @param signal the persisted domain signal
     * @return frame map ready for JSON serialization
     */
    public Map<String, Object> buildFrame(Signal signal) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("signal_id",             signal.getSignalId());
        payload.put("bot_id",                signal.getBotId());
        payload.put("symbol",                signal.getSymbol());
        payload.put("action",                signal.getAction() != null ? signal.getAction().name() : null);
        payload.put("market_type",           resolveMarketType(signal));
        payload.put("order_type",            resolveOrderType(signal));
        payload.put("entry",                 signal.getEntry());
        payload.put("stop_loss",             signal.getStopLoss());
        payload.put("take_profit",           signal.getTakeProfit());
        payload.put("amount",                signal.getAmount());
        payload.put("leverage",              signal.getLeverage() != null ? signal.getLeverage() : 1);
        payload.put("margin_mode",           resolveMarginMode(signal));
        payload.put("reduce_only",           signal.getReduceOnly());  // null = executor derives from action
        payload.put("status",                signal.getStatus() != null ? signal.getStatus().name() : null);
        payload.put("timeframe",             signal.getTimeframe());
        payload.put("cancel_after_timestamp", calculateExpiryEpoch(signal.getTimeframe(), signal.getMetadata()));
        payload.put("generated_timestamp",   signal.getGeneratedTimestamp() != null
                ? signal.getGeneratedTimestamp().toString() : null);
        payload.put("metadata",              signal.getMetadata());

        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type",    "signal");
        frame.put("payload", payload);
        return frame;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String resolveMarketType(Signal signal) {
        return signal.getMarketType() != null
                ? signal.getMarketType().name()
                : MarketType.SPOT.name();
    }

    private String resolveOrderType(Signal signal) {
        return signal.getOrderType() != null
                ? signal.getOrderType().name()
                : OrderType.LIMIT.name();
    }

    private String resolveMarginMode(Signal signal) {
        return signal.getMarginMode() != null
                ? signal.getMarginMode().name()
                : MarginMode.CROSS.name();
    }

    /**
     * Calculate the absolute Unix epoch (seconds) after which the executor should
     * discard the signal as stale.
     *
     * <p>Priority:
     * <ol>
     *   <li>Explicit {@code metadata.cancel_after_seconds} directive from the bot.</li>
     *   <li>One candle duration derived from {@code timeframe} (e.g. {@code "1h"} → 3 600 s).</li>
     *   <li>Default fallback: {@value DEFAULT_EXPIRY_SECONDS} seconds.</li>
     * </ol>
     */
    long calculateExpiryEpoch(String timeframe, Map<String, Object> metadata) {
        long nowEpoch = java.time.Instant.now().getEpochSecond();

        // 1. Explicit override from bot metadata
        if (metadata != null && metadata.containsKey("cancel_after_seconds")) {
            try {
                long custom = Long.parseLong(metadata.get("cancel_after_seconds").toString());
                if (custom > 0) {
                    return nowEpoch + custom;
                }
            } catch (NumberFormatException ignored) {
                log.warn("[SignalFrameBuilder] Invalid cancel_after_seconds in metadata, using timeframe fallback");
            }
        }

        // 2. Derive from timeframe string
        if (timeframe != null && !timeframe.isBlank()) {
            String cleaned = timeframe.toLowerCase().trim();
            try {
                if (cleaned.endsWith("m")) {
                    long minutes = Long.parseLong(cleaned.replace("m", ""));
                    return nowEpoch + (minutes * 60L);
                }
                if (cleaned.endsWith("h")) {
                    long hours = Long.parseLong(cleaned.replace("h", ""));
                    return nowEpoch + (hours * 3_600L);
                }
                if (cleaned.endsWith("d")) {
                    long days = Long.parseLong(cleaned.replace("d", ""));
                    return nowEpoch + (days * 86_400L);
                }
            } catch (NumberFormatException e) {
                log.warn("[SignalFrameBuilder] Cannot parse timeframe='{}', using default expiry", timeframe);
            }
        }

        // 3. Default
        return nowEpoch + DEFAULT_EXPIRY_SECONDS;
    }
}

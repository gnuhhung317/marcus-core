package io.marcus.domain.model;

import io.marcus.domain.vo.MarginMode;
import io.marcus.domain.vo.MarketType;
import io.marcus.domain.vo.OrderType;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Core domain model for a trading signal.
 *
 * <p>
 * Fields are designed to be 1-to-1 mappable to CCXT {@code create_order}
 * parameters, following the Unified Signal Specification (80/20 principle):
 * <ul>
 * <li>Risk management: {@code entry}, {@code stopLoss}, {@code takeProfit}</li>
 * <li>Position management: {@code leverage}, {@code marginMode}</li>
 * <li>Order safeguards: {@code reduceOnly}</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Signal extends BaseModel {

    /**
     * Unique signal identifier used for idempotency checks.
     */
    private String signalId;

    /**
     * Bot that generated this signal.
     */
    private String botId;

    /**
     * Canonical published transport symbol. Backend emits compact wire symbols
     * such as {@code BTCUSDT}; executors may normalize them internally for
     * exchange adapters.
     */
    private String symbol;

    /**
     * Canonical transport action. OPEN/CLOSE actions map to CCXT
     * {@code side} (buy/sell) and {@code params.reduceOnly}; {@code
     * UPDATE_TP_SL} updates protective orders without opening or closing a
     * position.
     */
    private SignalAction action;

    /**
     * Market type. Determines CCXT {@code defaultType} on the executor.
     * Defaults to {@link MarketType#SPOT} if null.
     */
    private MarketType marketType;

    /**
     * Order type. Determines whether {@code entry} price is used. Defaults to
     * {@link OrderType#LIMIT} if null.
     */
    private OrderType orderType;

    /**
     * Entry price. Required when {@code orderType = LIMIT}; informational for
     * MARKET. Maps to CCXT {@code price}.
     */
    private BigDecimal entry;

    /**
     * Stop-loss price. Maps to CCXT {@code params.stopPrice}
     * (exchange-specific).
     */
    private BigDecimal stopLoss;

    /**
     * Take-profit price. Maps to CCXT {@code params.stopPrice}
     * (exchange-specific).
     */
    private BigDecimal takeProfit;

    /**
     * Order size in base asset. If null, executor falls back to its configured
     * default. Maps to CCXT {@code amount}.
     */
    private BigDecimal amount;

    /**
     * Futures leverage multiplier (1-125). Executor calls
     * {@code exchange.set_leverage(leverage, symbol)} before placing order.
     * Ignored for SPOT. Defaults to 1 if null.
     */
    private Integer leverage;

    /**
     * Futures margin mode. Executor calls
     * {@code exchange.set_margin_mode(marginMode, symbol)} before placing
     * order. Ignored for SPOT. Defaults to {@link MarginMode#CROSS} if null.
     */
    private MarginMode marginMode;

    /**
     * Explicit reduce-only flag. If null, the executor derives it from
     * {@code action} ({@code CLOSE_LONG} / {@code CLOSE_SHORT} -> true; OPEN
     * variants -> false). Maps to CCXT {@code params.reduceOnly}.
     */
    private Boolean reduceOnly;

    /**
     * Signal lifecycle state.
     */
    private SignalStatus status;

    /**
     * UTC timestamp when the bot generated this signal.
     */
    private LocalDateTime generatedTimestamp;

    /**
     * Candle timeframe (e.g. {@code "1h"}, {@code "15m"}). Used to derive
     * expiry.
     */
    private String timeframe;

    /**
     * Arbitrary key-value metadata for extensibility.
     */
    private Map<String, Object> metadata;

    /**
     * Structured execution policies forwarded to executors (e.g. sizing,
     * deadlines).
     */
    private Map<String, Object> policies;

    public boolean simulated() {
        if (metadata == null) {
            return false;
        }
        Object simulationVal = metadata.get("simulation");
        if (simulationVal instanceof Boolean) {
            return (Boolean) simulationVal;
        }
        if (simulationVal instanceof String) {
            return Boolean.parseBoolean((String) simulationVal);
        }
        return false;
    }
}

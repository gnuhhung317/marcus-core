package io.marcus.domain.vo;

/**
 * Margin mode for futures positions.
 *
 * <ul>
 *   <li>{@code CROSS}    – entire wallet balance used as margin</li>
 *   <li>{@code ISOLATED} – dedicated margin capped to the position size</li>
 * </ul>
 *
 * Maps to CCXT {@code exchange.set_margin_mode(marginMode, symbol)}.
 */
public enum MarginMode {
    CROSS,
    ISOLATED
}

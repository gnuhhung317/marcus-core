package io.marcus.domain.vo;

/**
 * Order type for a signal.
 *
 * <ul>
 *   <li>{@code LIMIT}  – execute at the specified {@code entry} price or better</li>
 *   <li>{@code MARKET} – execute immediately at the best available price;
 *                        {@code entry} field is informational only</li>
 * </ul>
 *
 * Maps to CCXT {@code type} parameter in {@code create_order}.
 */
public enum OrderType {
    LIMIT,
    MARKET
}

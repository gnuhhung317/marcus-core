package io.marcus.domain.vo;

/**
 * Market type for a signal. Determines the CCXT {@code defaultType} used
 * when building the exchange client on the executor side.
 *
 * <ul>
 *   <li>{@code SPOT}   – regular spot market (default)</li>
 *   <li>{@code FUTURE} – perpetual or delivery futures (linear/inverse)</li>
 *   <li>{@code MARGIN} – margin / cross-collateral spot</li>
 * </ul>
 */
public enum MarketType {
    SPOT,
    FUTURE,
    MARGIN
}

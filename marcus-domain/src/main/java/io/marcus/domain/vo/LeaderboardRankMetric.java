package io.marcus.domain.vo;

public enum LeaderboardRankMetric {
    CAGR,
    SHARPE;

    /**
     * Parse from a string, defaulting to CAGR if unknown or null.
     */
    public static LeaderboardRankMetric fromString(String value) {
        if ("SHARPE".equalsIgnoreCase(value)) {
            return SHARPE;
        }
        return CAGR;
    }
}

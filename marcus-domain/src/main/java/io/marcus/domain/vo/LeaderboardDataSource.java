package io.marcus.domain.vo;

public enum LeaderboardDataSource {
    DRY_RUN,
    HISTORICAL;

    /**
     * Parse from a string, defaulting to DRY_RUN if unknown or null.
     */
    public static LeaderboardDataSource fromString(String value) {
        if ("HISTORICAL".equalsIgnoreCase(value)) {
            return HISTORICAL;
        }
        return DRY_RUN;
    }
}

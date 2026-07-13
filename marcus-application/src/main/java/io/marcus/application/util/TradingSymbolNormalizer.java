package io.marcus.application.util;

public final class TradingSymbolNormalizer {

    private static final String[] TRAILING_QUOTES = {
            "USDT", "USDC", "BUSD", "USD", "FDUSD", "TUSD", "BTC", "ETH", "BNB", "TRY", "EUR"
    };

    private TradingSymbolNormalizer() {
    }

    public static String normalize(String symbol) {
        if (symbol == null) {
            return null;
        }

        String raw = symbol.trim().toUpperCase();
        if (raw.isEmpty()) {
            return null;
        }

        if (raw.contains("/")) {
            return raw;
        }

        int suffixIndex = raw.indexOf(':');
        String baseAndQuote = suffixIndex >= 0 ? raw.substring(0, suffixIndex) : raw;
        String suffix = suffixIndex >= 0 ? raw.substring(suffixIndex + 1) : null;

        String normalized = normalizeWithoutSuffix(baseAndQuote);
        if (suffix == null || suffix.isBlank()) {
            return normalized;
        }

        return normalized + ":" + suffix.trim().toUpperCase();
    }

    private static String normalizeWithoutSuffix(String value) {
        for (String quote : TRAILING_QUOTES) {
            if (value.endsWith(quote) && value.length() > quote.length()) {
                return value.substring(0, value.length() - quote.length()) + "/" + quote;
            }
        }
        return value;
    }
}

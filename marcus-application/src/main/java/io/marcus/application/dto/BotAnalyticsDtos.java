package io.marcus.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BotAnalyticsDtos {

    private BotAnalyticsDtos() {
    }

    public record MetricBlock(
            double annualReturn,
            double maxDrawdown,
            double sharpe,
            double sortino,
            double calmar,
            double profitFactor,
            long sampleSizeDays,
            String statisticalSignificanceWarning
    ) {
    }

    public record GroupedMetricsResponse(
            MetricBlock total,
            MetricBlock historical,
            MetricBlock outOfSample
    ) {
    }

    public record PerformancePoint(
            LocalDateTime timestamp,
            double value,
            String phase
    ) {
    }

    public record PerformanceSeriesResponse(
            LocalDateTime splitTimestamp,
            List<PerformancePoint> points
    ) {
    }

    public record TelemetrySnapshot(
            LocalDateTime timestamp,
            BigDecimal equity,
            BigDecimal realizedPnl,
            BigDecimal unrealizedPnl
    ) {
    }
}

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
            BigDecimal unrealizedPnl,
            String metricsJson
    ) {
    }

    public record DryRunPortfolioSnapshot(
            LocalDateTime timestamp,
            BigDecimal cash,
            BigDecimal equity,
            BigDecimal realizedPnl,
            BigDecimal unrealizedPnl,
            BigDecimal totalFees
    ) {
    }

    public record DryRunPositionSnapshot(
            String positionId,
            String symbol,
            String marketType,
            String side,
            BigDecimal quantity,
            BigDecimal entryPrice,
            BigDecimal currentPrice,
            BigDecimal unrealizedPnl,
            LocalDateTime openedAt,
            String sourceSignalId,
            String status
    ) {
    }

    public record DryRunClosedTradeSnapshot(
            String tradeId,
            String symbol,
            String marketType,
            String side,
            BigDecimal quantity,
            BigDecimal entryPrice,
            BigDecimal exitPrice,
            BigDecimal pnl,
            BigDecimal fees,
            LocalDateTime entryTimestamp,
            LocalDateTime exitTimestamp,
            String entrySignalId,
            String exitSignalId
    ) {
    }

    public record DryRunStateResponse(
            DryRunPortfolioSnapshot portfolio,
            List<DryRunPositionSnapshot> positions,
            List<DryRunClosedTradeSnapshot> closedTrades
    ) {
    }
}

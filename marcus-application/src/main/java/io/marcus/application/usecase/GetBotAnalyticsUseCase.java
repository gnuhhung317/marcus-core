package io.marcus.application.usecase;

import io.marcus.application.dto.BotAnalyticsDtos.GroupedMetricsResponse;
import io.marcus.application.dto.BotAnalyticsDtos.MetricBlock;
import io.marcus.application.dto.BotAnalyticsDtos.PerformancePoint;
import io.marcus.application.dto.BotAnalyticsDtos.PerformanceSeriesResponse;
import io.marcus.domain.model.BotBacktestRun;
import io.marcus.domain.model.BotDryRunClosedTrade;
import io.marcus.domain.model.BotDryRunPortfolioPoint;
import io.marcus.domain.model.BotHistoricalClosedTrade;
import io.marcus.domain.port.BotBacktestPort;
import io.marcus.domain.port.BotDryRunPort;
import io.marcus.domain.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetBotAnalyticsUseCase {

    private static final int OOS_SIGNIFICANCE_DAYS = 30;

    private final BotRepository botRepository;
    private final BotBacktestPort botBacktestPort;
    private final BotDryRunPort botDryRunPort;

    public GroupedMetricsResponse getMetrics(String botId) {
        ensureBotExists(botId);
        List<CurvePoint> historical = historicalCurve(botId);
        List<CurvePoint> oos = oosCurve(botId, historical);
        List<CurvePoint> total = new ArrayList<>(historical);
        total.addAll(oos);
        total = total.stream().sorted(Comparator.comparing(CurvePoint::timestamp)).toList();

        TradeStats historicalTrades = historicalTradeStats(botId);
        TradeStats oosTrades = oosTradeStats(botId);
        TradeStats totalTrades = historicalTrades.merge(oosTrades);

        return new GroupedMetricsResponse(
                calculate(total, totalTrades, false),
                calculate(historical, historicalTrades, false),
                calculate(oos, oosTrades, true)
        );
    }

    public PerformanceSeriesResponse getPerformanceSeries(String botId, String range) {
        ensureBotExists(botId);
        List<CurvePoint> historical = historicalCurve(botId);
        List<CurvePoint> oos = oosCurve(botId, historical);
        LocalDateTime splitTimestamp = oos.isEmpty() ? null : oos.get(0).timestamp();

        List<PerformancePoint> points = new ArrayList<>();
        historical.forEach(point -> points.add(new PerformancePoint(point.timestamp(), round2(point.value()), "HISTORICAL")));
        oos.forEach(point -> points.add(new PerformancePoint(point.timestamp(), round2(point.value()), "OUT_OF_SAMPLE")));
        List<PerformancePoint> sortedPoints = points.stream().sorted(Comparator.comparing(PerformancePoint::timestamp)).toList();

        return new PerformanceSeriesResponse(splitTimestamp, applyRange(sortedPoints, range));
    }

    private void ensureBotExists(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId is required");
        }
        botRepository.findByBotId(botId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Bot not found: " + botId));
    }

    private List<CurvePoint> historicalCurve(String botId) {
        return botBacktestPort.findLatestRun(botId.trim())
                .map(this::historicalCurveForRun)
                .orElse(List.of());
    }

    private List<CurvePoint> historicalCurveForRun(BotBacktestRun run) {
        List<BotDryRunPortfolioPoint> points = botBacktestPort.findPortfolioPoints(run.botId(), run.runId()).stream()
                .sorted(Comparator.comparing(BotDryRunPortfolioPoint::timestamp))
                .toList();
        if (points.isEmpty()) {
            return List.of();
        }
        double firstEquity = points.get(0).equity().doubleValue();
        if (firstEquity == 0.0d) {
            return List.of();
        }
        return points.stream()
                .map(point -> new CurvePoint(
                        point.timestamp(),
                        ((point.equity().doubleValue() - firstEquity) / Math.abs(firstEquity)) * 100.0
                ))
                .toList();
    }

    private List<CurvePoint> oosCurve(String botId, List<CurvePoint> historical) {
        List<BotDryRunPortfolioPoint> snapshots = botDryRunPort.findPortfolioPoints(botId.trim()).stream()
                .sorted(Comparator.comparing(BotDryRunPortfolioPoint::timestamp))
                .toList();
        if (snapshots.isEmpty()) {
            return List.of();
        }

        double firstEquity = snapshots.get(0).equity().doubleValue();
        if (firstEquity == 0.0d) {
            return List.of();
        }

        return snapshots.stream()
                .map(point -> new CurvePoint(
                        point.timestamp(),
                        ((point.equity().doubleValue() - firstEquity) / Math.abs(firstEquity)) * 100.0
                ))
                .toList();
    }

    private List<PerformancePoint> applyRange(List<PerformancePoint> points, String range) {
        if (points.isEmpty() || range == null || range.isBlank() || "ALL".equalsIgnoreCase(range)) {
            return points;
        }
        LocalDateTime end = points.get(points.size() - 1).timestamp();
        long days = switch (range.trim().toUpperCase()) {
            case "1D" -> 1;
            case "1W" -> 7;
            case "1M" -> 30;
            case "YTD" -> Math.max(1, ChronoUnit.DAYS.between(LocalDateTime.of(end.getYear(), 1, 1, 0, 0), end));
            default -> Long.MAX_VALUE;
        };
        if (days == Long.MAX_VALUE) {
            return points;
        }
        LocalDateTime from = end.minusDays(days);
        return points.stream().filter(point -> !point.timestamp().isBefore(from)).toList();
    }

    private MetricBlock calculate(List<CurvePoint> points, TradeStats tradeStats, boolean oos) {
        if (points == null || points.size() < 2) {
            return new MetricBlock(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    round4(tradeStats.winRate()),
                    0,
                    tradeStats.totalTrades(),
                    oos ? "Not enough data for statistical significance" : null
            );
        }

        long sampleDays = Math.max(1, ChronoUnit.DAYS.between(points.get(0).timestamp(), points.get(points.size() - 1).timestamp()) + 1);
        double startingEquity = normalizedEquity(points.get(0).value());
        double endingEquity = normalizedEquity(points.get(points.size() - 1).value());
        double annualReturn = calculateCagr(startingEquity, endingEquity, sampleDays);
        double maxDrawdown = maxDrawdown(points);
        double volatility = volatility(points);
        double sharpe = volatility == 0.0d ? 0.0d : annualReturn / volatility;
        double sortino = sharpe;
        double calmar = maxDrawdown == 0.0d ? 0.0d : annualReturn / Math.abs(maxDrawdown);
        double profitFactor = profitFactor(points);
        String warning = oos && sampleDays < OOS_SIGNIFICANCE_DAYS ? "Not enough data for statistical significance" : null;

        return new MetricBlock(
                round4(annualReturn),
                round4(-Math.abs(maxDrawdown)),
                round4(sharpe),
                round4(sortino),
                round4(calmar),
                round4(profitFactor),
                round4(tradeStats.winRate()),
                sampleDays,
                tradeStats.totalTrades(),
                warning
        );
    }

    private double maxDrawdown(List<CurvePoint> points) {
        double peakEquity = normalizedEquity(points.get(0).value());
        double maxDrawdown = 0.0d;
        for (CurvePoint point : points) {
            double equity = normalizedEquity(point.value());
            peakEquity = Math.max(peakEquity, equity);
            if (peakEquity > 0.0d) {
                double drawdown = (peakEquity - equity) / peakEquity;
                maxDrawdown = Math.max(maxDrawdown, drawdown);
            }
        }
        return maxDrawdown;
    }

    private double normalizedEquity(double returnPercent) {
        return 1.0d + (returnPercent / 100.0d);
    }

    private double calculateCagr(double startingEquity, double endingEquity, long sampleDays) {
        if (startingEquity <= 0.0d || sampleDays <= 0) {
            return 0.0d;
        }

        if (endingEquity <= 0.0d) {
            return -1.0d;
        }

        return Math.pow((endingEquity / startingEquity), (365.0d / sampleDays)) - 1.0d;
    }

    private double volatility(List<CurvePoint> points) {
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            returns.add((points.get(i).value() - points.get(i - 1).value()) / 100.0);
        }
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
        double variance = returns.stream().mapToDouble(value -> Math.pow(value - mean, 2)).average().orElse(0.0d);
        return Math.sqrt(variance) * Math.sqrt(Math.max(1, returns.size()));
    }

    private double profitFactor(List<CurvePoint> points) {
        double gains = 0.0d;
        double losses = 0.0d;
        for (int i = 1; i < points.size(); i++) {
            double delta = points.get(i).value() - points.get(i - 1).value();
            if (delta >= 0) {
                gains += delta;
            } else {
                losses += Math.abs(delta);
            }
        }
        return losses == 0.0d ? gains : gains / losses;
    }

    private double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private double round4(double value) {
        return Math.round(value * 10_000.0d) / 10_000.0d;
    }

    private TradeStats historicalTradeStats(String botId) {
        return botBacktestPort.findLatestRun(botId.trim())
                .map(run -> historicalTradeStats(botBacktestPort.findClosedTrades(botId.trim(), run.runId())))
                .orElse(TradeStats.empty());
    }

    private TradeStats oosTradeStats(String botId) {
        return dryRunTradeStats(botDryRunPort.findClosedTrades(botId.trim()));
    }

    private TradeStats historicalTradeStats(List<BotHistoricalClosedTrade> trades) {
        if (trades == null || trades.isEmpty()) {
            return TradeStats.empty();
        }

        long winningTrades = trades.stream()
                .map(BotHistoricalClosedTrade::pnl)
                .mapToDouble(value -> value == null ? 0.0d : value.doubleValue())
                .filter(value -> value > 0.0d)
                .count();
        return new TradeStats(trades.size(), winningTrades);
    }

    private TradeStats dryRunTradeStats(List<BotDryRunClosedTrade> trades) {
        if (trades == null || trades.isEmpty()) {
            return TradeStats.empty();
        }

        long winningTrades = trades.stream()
                .map(BotDryRunClosedTrade::pnl)
                .mapToDouble(value -> value == null ? 0.0d : value.doubleValue())
                .filter(value -> value > 0.0d)
                .count();
        return new TradeStats(trades.size(), winningTrades);
    }

    private record CurvePoint(LocalDateTime timestamp, double value) {
    }

    private record TradeStats(long totalTrades, long winningTrades) {

        static TradeStats empty() {
            return new TradeStats(0, 0);
        }

        double winRate() {
            return totalTrades == 0 ? 0.0d : winningTrades / (double) totalTrades;
        }

        TradeStats merge(TradeStats other) {
            return new TradeStats(totalTrades + other.totalTrades, winningTrades + other.winningTrades);
        }
    }
}

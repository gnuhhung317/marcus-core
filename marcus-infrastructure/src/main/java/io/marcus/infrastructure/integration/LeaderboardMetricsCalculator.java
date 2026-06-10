package io.marcus.infrastructure.integration;

import io.marcus.domain.model.BotBacktestRun;
import io.marcus.domain.model.BotDryRunPortfolioPoint;
import io.marcus.domain.port.BotBacktestPort;
import io.marcus.domain.port.BotDryRunPort;
import io.marcus.domain.port.LeaderboardMetricsRefreshPort;
import io.marcus.domain.service.EquityCurveMetricsCalculator;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.LeaderboardDataSource;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataLeaderboardMetricsRepository;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job that recalculates leaderboard metrics for all active bots. Runs
 * every 5 minutes to keep leaderboard data fresh.
 *
 * <p>
 * Key design decisions:</p>
 * <ul>
 * <li>NO @Transactional at method level - each saveOrUpdate is a separate short
 * transaction</li>
 * <li>Try-catch inside loop - one bot failure doesn't stop the entire
 * batch</li>
 * <li>Priority: DRY_RUN first (for Main Leaderboard), then HISTORICAL (for
 * Proving Grounds)</li>
 * <li>Minimum 7 days required for DRY_RUN to appear in Main Leaderboard</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardMetricsCalculator implements LeaderboardMetricsRefreshPort {

    private static final int MIN_DRY_RUN_DAYS = 7;  // Minimum days for Main Leaderboard

    private final BotDryRunPort botDryRunPort;
    private final BotBacktestPort botBacktestPort;
    private final SpringDataBotRepository botRepository;
    private final EquityCurveMetricsCalculator metricsCalculator;
    private final SpringDataLeaderboardMetricsRepository metricsRepository;

    /**
     * Recalculate metrics for all active bots every 5 minutes. Each bot's
     * metrics are saved in a separate short transaction.
     */
    @Scheduled(cron = "0 0 2 * * *")
    //  NO @Transactional here - each saveOrUpdate has its own short transaction
    public void recalculateAllMetrics() {
        log.info("Starting leaderboard metrics recalculation...");
        long startTime = System.currentTimeMillis();

        List<BotEntity> activeBots = botRepository.findByStatusNot(BotStatus.DELETED);
        log.info("Found {} active bots to process", activeBots.size());

        int successCount = 0;
        int errorCount = 0;
        int skippedCount = 0;

        for (BotEntity bot : activeBots) {
            try {
                boolean processed = processBot(bot);
                if (processed) {
                    successCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                errorCount++;
                log.error("Failed to calculate metrics for bot: {}", bot.getBotId(), e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Completed leaderboard metrics recalculation in {}ms. Success: {}, Skipped: {}, Errors: {}",
                duration, successCount, skippedCount, errorCount);
    }

    /**
     * Process a single bot and save its metrics.
     *
     * @param bot The bot entity to process
     * @return true if metrics were saved, false if no data available
     */
    private boolean processBot(BotEntity bot) {
        String botId = bot.getBotId();

        // Priority 1: Try DRY_RUN (live OOS data) for Main Leaderboard
        List<BotDryRunPortfolioPoint> dryRunPoints = botDryRunPort.findPortfolioPoints(botId);

        if (!dryRunPoints.isEmpty() && dryRunPoints.size() >= 2) {
            LocalDateTime firstTimestamp = dryRunPoints.get(0).timestamp();
            LocalDateTime lastTimestamp = dryRunPoints.get(dryRunPoints.size() - 1).timestamp();
            long days = ChronoUnit.DAYS.between(firstTimestamp, lastTimestamp) + 1;

            // Only include in Main Leaderboard if minimum 7 days
            if (days >= MIN_DRY_RUN_DAYS) {
                EquityCurveMetricsCalculator.MetricsResult metrics = metricsCalculator.calculate(dryRunPoints);

                //  Each saveOrUpdate is a separate short transaction
                metricsRepository.saveOrUpdate(
                        botId,
                        LeaderboardDataSource.DRY_RUN.name(),
                        metrics.annualReturn(),
                        metrics.sharpe(),
                        metrics.maxDrawdown(),
                        days
                );
                log.debug("Saved DRY_RUN metrics for bot {}: CAGR={}, Sharpe={}, Days={}",
                        botId, metrics.annualReturn(), metrics.sharpe(), days);
                return true;
            }
        }

        // Priority 2: Fallback to HISTORICAL (backtest data) for Proving Grounds
        return botBacktestPort.findLatestRun(botId)
                .map(run -> {
                    List<BotDryRunPortfolioPoint> points = botBacktestPort.findPortfolioPoints(botId, run.runId());
                    if (points.isEmpty()) {
                        return false;
                    }

                    EquityCurveMetricsCalculator.MetricsResult metrics = metricsCalculator.calculate(points);
                    long days = ChronoUnit.DAYS.between(
                            points.get(0).timestamp(),
                            points.get(points.size() - 1).timestamp()
                    ) + 1;

                    //  Each saveOrUpdate is a separate short transaction
                    metricsRepository.saveOrUpdate(
                            botId,
                            LeaderboardDataSource.HISTORICAL.name(),
                            metrics.annualReturn(),
                            metrics.sharpe(),
                            metrics.maxDrawdown(),
                            days
                    );
                    log.debug("Saved HISTORICAL metrics for bot {}: CAGR={}, Sharpe={}, Days={}",
                            botId, metrics.annualReturn(), metrics.sharpe(), days);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Recalculate metrics for a specific bot (useful for on-demand updates).
     * Called when a bot's backtest is uploaded or dry-run data changes
     * significantly.
     *
     * @param botId The bot identifier
     */
    public void recalculateForBot(String botId) {
        log.info("Recalculating metrics for bot: {}", botId);
        botRepository.findByBotId(botId)
                .ifPresentOrElse(
                        this::processBot,
                        () -> log.warn("Bot not found for metrics recalculation: {}", botId)
                );
    }
}

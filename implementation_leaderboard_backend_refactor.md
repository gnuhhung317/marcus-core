# Implementation Plan — Leaderboard Backend Refactor

[Overview]
Replace hardcoded mock data in `StaticBotDiscoveryReadAdapter` with real database-driven implementations. Use `BotDryRunPortfolioPoint` (real equity curve) as primary data source and `SignalMetricsCalculator` (signals with entry/tp/sl) as secondary when dry-run is missing. No silent fallbacks — bots without any data are excluded with warnings.

**Why this is needed:**
- Frontend redesign expects real strategy data sorted by CAGR or Sharpe.
- Backend returns only 3 hardcoded entries with fabricated metrics.
- `GetBotAnalyticsUseCase.historicalCurve()` returns hardcoded mock data (silent fallback).
- Signals are stateless events routed through backend; they cannot reconstruct equity curves.

**Data source reality:**
- **Primary**: `BotDryRunPortfolioPoint` — equity curve synced by bots via `/api/v1/bots/{botId}/dry-run/sync`. Has `equity` value at each timestamp.
- **Secondary**: Signals (entry/tp/sl) — use `SignalMetricsCalculator` to compute avg per-signal return, max drawdown, sharpe. NOT a real equity curve, but a metric derived from signal TP/SL distances.
- **Missing data**: Exclude bot from leaderboard + log warning.

**Scope:**
- 3 files modified, 1 file created, 0 files deleted.
- Domain model: Add optional `dataSource` field to `LeaderboardStrategySnapshot`.
- No frontend changes required (field is optional).

[Types]
**Modified domain record:**
- `LeaderboardStrategySnapshot(rank, strategyId, strategyName, creatorName, cagr, sharpe, maxDrawdown, dataSource)` — Add optional `dataSource` field: `"DRY_RUN"` or `"SIGNAL_BASED"` (no "COMBINED" since dry-run and signals are different calculation models, not combinable).

[Files]
**New files:**
- `signal-core-backend/marcus-domain/src/main/java/io/marcus/domain/service/EquityCurveMetricsCalculator.java` — Computes metrics (annualReturn, maxDrawdown, sharpe) from equity curve points. Extracted from `GetBotAnalyticsUseCase.calculate()`.

**Modified files:**
- `signal-core-backend/marcus-domain/src/main/java/io/marcus/domain/port/BotDiscoveryReadPort.java` — Add `dataSource` field to `LeaderboardStrategySnapshot` record.
- `signal-core-backend/marcus-application/src/main/java/io/marcus/application/usecase/GetBotAnalyticsUseCase.java` — Refactor to use `EquityCurveMetricsCalculator`. Remove hardcoded `historicalCurve()`; if no OOS data, return empty curve + warning (not mock).
- `signal-core-backend/marcus-infrastructure/src/main/java/io/marcus/infrastructure/integration/StaticBotDiscoveryReadAdapter.java` — Replace 3 hardcoded leaderboard methods with real DB queries.

[Functions]
**New in `EquityCurveMetricsCalculator.java`:**

1. `public EquityCurveMetricsCalculator()` — Stateless component.

2. `public MetricsResult calculate(List<CurvePoint> points)` — Computes: annualReturn, maxDrawdown, sharpe, sampleDays from equity curve points. Returns `MetricsResult` record.

3. `public record CurvePoint(LocalDateTime timestamp, double value)` — Equity curve point.

4. `public record MetricsResult(double annualReturn, double maxDrawdown, double sharpe, long sampleDays, String warning)` — Calculation result.

**Modified in `BotDiscoveryReadPort.java`:**

1. `LeaderboardStrategySnapshot` — Add `String dataSource` field (nullable for backward compat).

**Modified in `GetBotAnalyticsUseCase.java`:**

1. Add field: `private final EquityCurveMetricsCalculator equityCurveMetricsCalculator;`
2. Refactor `calculate(List<CurvePoint>, boolean)` to delegate.
3. Replace `historicalCurve()` with empty list (no more mock). If OOS curve is empty, metrics will show "No data available" warning. Log warning at startup if no historical source configured.
4. `getMetrics()`: If both historical and oos are empty, return metric blocks with warning messages (not mock values).

**Modified in `StaticBotDiscoveryReadAdapter.java`:**

1. Add fields: `private final BotDryRunPort botDryRunPort;`, `private final EquityCurveMetricsCalculator equityCurveMetricsCalculator;`, `private final SpringDataSignalRepository signalRepository;`, `private final SpringDataUserRepository userRepository;`
2. `listLeaderboardStrategies(timeframe, market, asset, rankMetric, page, size)`:
   - Fetch all bots via `botRepository.findAll()`. Filter out `BotStatus.DELETED`.
   - For each bot, determine data source:
     a. **Dry-run first**: Fetch `botDryRunPort.findPortfolioPoints(botId)`. If non-empty: convert to `CurvePoint(timestamp, equity)`, calculate metrics via `equityCurveMetricsCalculator`, set `dataSource = "DRY_RUN"`.
     b. **Signal-based fallback**: If no dry-run data, fetch signals via `signalRepository.findByBotIdAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(botId)`. Convert to `SignalMetricsCalculator.SignalData`. Call `SignalMetricsCalculator.calculate(signals, ageDays)`. Set `dataSource = "SIGNAL_BASED"`. Map `MetricsResult.annualReturn` → `cagr`, `MetricsResult.sharpe` → `sharpe`, `MetricsResult.maxDrawdown` → `maxDrawdown`.
     c. **No data**: Log warning "Bot {botId} excluded from leaderboard: no dry-run or signal data". Skip.
   - Build `LeaderboardStrategySnapshot` with real metrics and `dataSource`.
   - Resolve `creatorName` from `userRepository.findByUserId(bot.developerId)` → `user.username`. Fallback: "Unknown".
   - Sort by `rankMetric` (CAGR desc or Sharpe desc).
   - Assign ranks 1, 2, 3...
   - Paginate and return `LeaderboardStrategiesPageSnapshot`.
3. `listLeaderboardFeatured()` — Delegate: get top 3 by CAGR from `listLeaderboardStrategies(..., "CAGR", 0, 3)`. Map to `LeaderboardFeaturedItemSnapshot`.
4. `listLeaderboardSpotlights()` — Delegate: get top 5 by CAGR. Map to `StrategySpotlightSnapshot`. Set `market = "CRYPTO"` (bot has `tradingPair` but no market field), `oneDayReturn = cagr` (no separate 1d metric).

[Classes]
**New: `EquityCurveMetricsCalculator`**
- Package: `io.marcus.domain.service`
- `@Component`
- Stateless
- Methods: `calculate(List<CurvePoint>)`
- Records: `CurvePoint`, `MetricsResult`

**Modified: `BotDiscoveryReadPort`**
- Add `String dataSource` to `LeaderboardStrategySnapshot`

**Modified: `GetBotAnalyticsUseCase`**
- Use `EquityCurveMetricsCalculator`
- Remove hardcoded `historicalCurve()` — return empty if no data

**Modified: `StaticBotDiscoveryReadAdapter`**
- Inject `BotDryRunPort`, `EquityCurveMetricsCalculator`, `SpringDataSignalRepository`, `SpringDataUserRepository`
- Replace 3 method bodies

[Dependencies]
No new Maven dependencies. `SpringDataSignalRepository` already exists.

[Testing]
1. `mvn -pl marcus-domain -am test` — `EquityCurveMetricsCalculator` compiles.
2. `mvn -pl marcus-application -am test` — `GetBotAnalyticsUseCase` tests pass.
3. `mvn -pl marcus-infrastructure -am test` — Adapter compiles.
4. Start backend. Call `GET /api/v1/leaderboard/strategies` — returns real bots with `dataSource` field.
5. Bot with dry-run data → `dataSource: "DRY_RUN"`, metrics from equity curve.
6. Bot with only signals → `dataSource: "SIGNAL_BASED"`, metrics from `SignalMetricsCalculator`.
7. Bot with no data → excluded, warning logged.
8. `GET /api/v1/bots/{botId}/analytics/metrics` — still works, using shared calculator.

[Implementation Order]
1. **Create `EquityCurveMetricsCalculator.java`** — Extract from `GetBotAnalyticsUseCase`.
2. **Modify `BotDiscoveryReadPort.java`** — Add `dataSource` field.
3. **Modify `GetBotAnalyticsUseCase.java`** — Use shared calculator, remove hardcoded curve.
4. **Modify `StaticBotDiscoveryReadAdapter.java`** — Real DB queries with dual-source metrics.
5. **Build and verify**.
> **Status**: Superseded by the dual-pipeline bot lifecycle analytics feature.
> Keep this document only as historical planning context; the current source of truth for bot performance data is `signal-core-backend/CONTEXT.md` and `signal-core-backend/README.md`.

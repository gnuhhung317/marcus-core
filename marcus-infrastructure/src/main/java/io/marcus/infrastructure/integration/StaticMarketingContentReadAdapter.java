package io.marcus.infrastructure.integration;

import io.marcus.domain.port.MarketingContentReadPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Component
public class StaticMarketingContentReadAdapter implements MarketingContentReadPort {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 4, 1, 8, 0);

    private static final List<MarketTickerSnapshot> MARKET_TICKERS = List.of(
            new MarketTickerSnapshot("BTC/USDT", "Bitcoin", 64_231.50, 2.45),
            new MarketTickerSnapshot("ETH/USDT", "Ethereum", 3_412.12, -1.12),
            new MarketTickerSnapshot("SOL/USDT", "Solana", 148.82, -0.84),
            new MarketTickerSnapshot("BNB/USDT", "BNB", 598.23, 0.68),
            new MarketTickerSnapshot("AVAX/USDT", "Avalanche", 45.11, 1.07)
    );

    private static final List<AcademyCourseSummarySnapshot> ACADEMY_COURSES = List.of(
            new AcademyCourseSummarySnapshot("course-101", "Market Structure Foundations", "FOUNDATION", 78, 12, 14.5),
            new AcademyCourseSummarySnapshot("course-102", "Systematic Risk Playbook", "FOUNDATION", 64, 10, 11.0),
            new AcademyCourseSummarySnapshot("course-201", "Momentum Strategy Engineering", "ADVANCED", 46, 14, 18.0),
            new AcademyCourseSummarySnapshot("course-202", "Mean Reversion in Practice", "ADVANCED", 39, 11, 13.5),
            new AcademyCourseSummarySnapshot("course-203", "Portfolio Heatmap Diagnostics", "ADVANCED", 52, 9, 10.0),
            new AcademyCourseSummarySnapshot("course-301", "Cross-Venue Execution Design", "EXPERT", 33, 16, 22.0),
            new AcademyCourseSummarySnapshot("course-302", "Latency-Aware Routing Systems", "EXPERT", 28, 15, 20.5),
            new AcademyCourseSummarySnapshot("course-303", "Adaptive Position Sizing", "EXPERT", 59, 13, 15.0)
    );

    private static final List<BlogPostSummarySnapshot> BLOG_POSTS = List.of(
            new BlogPostSummarySnapshot(
                    "post-001",
                    "Why Regime Detection Matters Before You Deploy",
                    "Strategy",
                    "A practical guide to identifying market regimes before switching bot templates.",
                    "Marcus Research Desk",
                    publishedAt(2),
                    7
            ),
            new BlogPostSummarySnapshot(
                    "post-002",
                    "Sizing Rules That Keep Drawdown Tolerable",
                    "Risk",
                    "Position sizing patterns used to cap portfolio stress under volatility spikes.",
                    "Linh Tran",
                    publishedAt(4),
                    6
            ),
            new BlogPostSummarySnapshot(
                    "post-003",
                    "Interpreting Funding and Basis for Swing Signals",
                    "Macro",
                    "How derivatives context can improve confidence scoring in discretionary windows.",
                    "Quang Le",
                    publishedAt(6),
                    8
            ),
            new BlogPostSummarySnapshot(
                    "post-004",
                    "Execution Drift: The Hidden Cost in Backtests",
                    "Execution",
                    "Backtest alpha often erodes in production because of slippage and queue position.",
                    "Marcus Research Desk",
                    publishedAt(8),
                    5
            ),
            new BlogPostSummarySnapshot(
                    "post-005",
                    "Building a Weekly Bot Review Ritual",
                    "Operations",
                    "A checklist to review live bots with objective metrics and clear actions.",
                    "An Nguyen",
                    publishedAt(11),
                    4
            ),
            new BlogPostSummarySnapshot(
                    "post-006",
                    "When to Reduce Risk Instead of Chasing Recovery",
                    "Risk",
                    "Signals that indicate strategy degradation and why cutting risk can outperform revenge trades.",
                    "Linh Tran",
                    publishedAt(13),
                    6
            ),
            new BlogPostSummarySnapshot(
                    "post-007",
                    "Feature Flags for Safe Strategy Iteration",
                    "Engineering",
                    "Roll out bot changes safely with staged exposure and rollback controls.",
                    "Phong Bui",
                    publishedAt(15),
                    7
            ),
            new BlogPostSummarySnapshot(
                    "post-008",
                    "Reading Correlation Clusters in Crypto",
                    "Macro",
                    "Identify crowded beta and diversify capital allocation across uncorrelated groups.",
                    "Quang Le",
                    publishedAt(18),
                    8
            ),
            new BlogPostSummarySnapshot(
                    "post-009",
                    "Bot Telemetry Alerts You Should Not Ignore",
                    "Operations",
                    "Operational alert thresholds that catch silent failures before PnL impact grows.",
                    "Marcus Research Desk",
                    publishedAt(20),
                    5
            ),
            new BlogPostSummarySnapshot(
                    "post-010",
                    "Designing Strategy Briefs for Better Team Alignment",
                    "Strategy",
                    "A concise strategy brief format that helps research and execution teams move faster.",
                    "An Nguyen",
                    publishedAt(23),
                    5
            ),
            new BlogPostSummarySnapshot(
                    "post-011",
                    "Using Volatility Buckets to Control Leverage",
                    "Risk",
                    "Map leverage caps to volatility buckets to avoid overexposure during unstable sessions.",
                    "Linh Tran",
                    publishedAt(25),
                    7
            ),
            new BlogPostSummarySnapshot(
                    "post-012",
                    "A Practical Workflow for Daily Market Prep",
                    "Macro",
                    "Combine event calendars, liquidity checks, and momentum scans in one repeatable flow.",
                    "Quang Le",
                    publishedAt(28),
                    6
            )
    );

    private static final List<ResearchReportSummarySnapshot> RESEARCH_REPORTS = List.of(
            new ResearchReportSummarySnapshot(
                    "report-001",
                    "Quarterly Cross-Exchange Liquidity Review",
                    "Execution",
                    "Depth and spread comparison across major venues under peak and off-peak sessions.",
                    publishedAt(3),
                    10
            ),
            new ResearchReportSummarySnapshot(
                    "report-002",
                    "Factor Rotation in Crypto Majors",
                    "Macro",
                    "How momentum, carry, and mean-reversion factors rotated over recent quarters.",
                    publishedAt(6),
                    12
            ),
            new ResearchReportSummarySnapshot(
                    "report-003",
                    "Volatility Regime Heatmap and Allocation Rules",
                    "Risk",
                    "Recommended allocation ceilings by volatility regime and strategy class.",
                    publishedAt(8),
                    9
            ),
            new ResearchReportSummarySnapshot(
                    "report-004",
                    "Perpetual Funding Stress Indicator",
                    "Derivatives",
                    "A composite indicator linking funding dispersion and short-term reversal probability.",
                    publishedAt(10),
                    8
            ),
            new ResearchReportSummarySnapshot(
                    "report-005",
                    "Market Microstructure Drift Monitoring",
                    "Execution",
                    "Signal quality changes caused by spread widening and order book imbalance.",
                    publishedAt(13),
                    11
            ),
            new ResearchReportSummarySnapshot(
                    "report-006",
                    "Stablecoin Flow and Risk Sentiment",
                    "Macro",
                    "On-chain stablecoin issuance and exchange inflow trends as risk sentiment proxies.",
                    publishedAt(16),
                    9
            ),
            new ResearchReportSummarySnapshot(
                    "report-007",
                    "Adaptive Stop Framework Evaluation",
                    "Risk",
                    "Benchmarking fixed vs adaptive stop logic across trend and chop environments.",
                    publishedAt(18),
                    10
            ),
            new ResearchReportSummarySnapshot(
                    "report-008",
                    "Cross-Asset Correlation Breakdown",
                    "Macro",
                    "When BTC decouples from equities and what that means for portfolio hedging.",
                    publishedAt(21),
                    11
            ),
            new ResearchReportSummarySnapshot(
                    "report-009",
                    "Slippage Attribution by Venue",
                    "Execution",
                    "A decomposition of realized slippage into latency, spread, and queue components.",
                    publishedAt(24),
                    8
            ),
            new ResearchReportSummarySnapshot(
                    "report-010",
                    "Derivatives Positioning Watchlist",
                    "Derivatives",
                    "Positioning extremes that historically preceded volatility expansion.",
                    publishedAt(27),
                    7
            )
    );

    private static final List<ResearchLibraryFileSnapshot> RESEARCH_LIBRARY_FILES = List.of(
            new ResearchLibraryFileSnapshot("file-001", "Liquidity-Review-Q1", "PDF", 2.1),
            new ResearchLibraryFileSnapshot("file-002", "Factor-Rotation-Playbook", "PDF", 1.9),
            new ResearchLibraryFileSnapshot("file-003", "Volatility-Regime-Heatmap", "PDF", 2.4),
            new ResearchLibraryFileSnapshot("file-004", "Funding-Stress-Dashboard", "XLSX", 1.3),
            new ResearchLibraryFileSnapshot("file-005", "Microstructure-Drift-Notes", "PDF", 1.8),
            new ResearchLibraryFileSnapshot("file-006", "Stablecoin-Flow-Brief", "PDF", 1.5),
            new ResearchLibraryFileSnapshot("file-007", "Adaptive-Stop-Appendix", "DOCX", 0.9),
            new ResearchLibraryFileSnapshot("file-008", "Correlation-Breakdown", "PDF", 1.7),
            new ResearchLibraryFileSnapshot("file-009", "Slippage-Attribution", "PDF", 2.0),
            new ResearchLibraryFileSnapshot("file-010", "Derivatives-Watchlist", "XLSX", 1.1)
    );

    @Override
    public MarketOverviewSnapshot getMarketOverview() {
        return new MarketOverviewSnapshot(245_000_000.0, 126, MARKET_TICKERS);
    }

    @Override
    public AcademyCoursesSnapshot listAcademyCourses(int limit) {
        int normalizedLimit = normalizeLimit(limit, 12, 50);
        int toIndex = Math.min(normalizedLimit, ACADEMY_COURSES.size());
        return new AcademyCoursesSnapshot(ACADEMY_COURSES.subList(0, toIndex));
    }

    @Override
    public AcademyMetricsSnapshot getAcademyMetrics() {
        return new AcademyMetricsSnapshot(12_480, 342, 17.8, 4.7);
    }

    @Override
    public BlogPostsPageSnapshot listBlogPosts(int page, int size, String query, String category) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size, 12, 100);
        String normalizedQuery = normalizeOptional(query);
        String normalizedCategory = normalizeOptional(category);

        List<BlogPostSummarySnapshot> filtered = BLOG_POSTS.stream()
                .filter(item -> matchesCategory(item.category(), normalizedCategory))
                .filter(item -> matchesBlogQuery(item, normalizedQuery))
                .toList();

        return new BlogPostsPageSnapshot(
                pageSlice(filtered, normalizedPage, normalizedSize),
                normalizedPage,
                normalizedSize,
                filtered.size()
        );
    }

    @Override
    public ResearchReportsPageSnapshot listResearchReports(int page, int size, String category) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size, 12, 100);
        String normalizedCategory = normalizeOptional(category);

        List<ResearchReportSummarySnapshot> filtered = RESEARCH_REPORTS.stream()
                .filter(item -> matchesCategory(item.category(), normalizedCategory))
                .toList();

        return new ResearchReportsPageSnapshot(
                pageSlice(filtered, normalizedPage, normalizedSize),
                normalizedPage,
                normalizedSize,
                filtered.size()
        );
    }

    @Override
    public List<ResearchLibraryFileSnapshot> listResearchLibraryFiles(int limit) {
        int normalizedLimit = normalizeLimit(limit, 8, 50);
        int toIndex = Math.min(normalizedLimit, RESEARCH_LIBRARY_FILES.size());
        return RESEARCH_LIBRARY_FILES.subList(0, toIndex);
    }

    private static LocalDateTime publishedAt(int daysAgo) {
        return BASE_TIME.minusDays(daysAgo);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size, int defaultSize, int maxSize) {
        if (size <= 0) {
            return defaultSize;
        }
        return Math.min(size, maxSize);
    }

    private int normalizeLimit(int limit, int defaultLimit, int maxLimit) {
        if (limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, maxLimit);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean matchesCategory(String sourceCategory, String expectedCategory) {
        return expectedCategory == null || sourceCategory.equalsIgnoreCase(expectedCategory);
    }

    private boolean matchesBlogQuery(BlogPostSummarySnapshot item, String query) {
        if (query == null) {
            return true;
        }

        String token = query.toLowerCase(Locale.ROOT);
        return containsToken(item.title(), token)
                || containsToken(item.excerpt(), token)
                || containsToken(item.authorName(), token)
                || containsToken(item.category(), token);
    }

    private boolean containsToken(String value, String token) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(token);
    }

    private <T> List<T> pageSlice(List<T> source, int page, int size) {
        long fromLong = (long) page * size;
        if (fromLong >= source.size()) {
            return List.of();
        }

        int fromIndex = (int) fromLong;
        int toIndex = Math.min(fromIndex + size, source.size());
        return source.subList(fromIndex, toIndex);
    }
}
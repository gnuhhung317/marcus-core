package io.marcus.domain.port;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketingContentReadPort {

    MarketOverviewSnapshot getMarketOverview();

    AcademyCoursesSnapshot listAcademyCourses(int limit);

    AcademyMetricsSnapshot getAcademyMetrics();

    BlogPostsPageSnapshot listBlogPosts(int page, int size, String query, String category);

    ResearchReportsPageSnapshot listResearchReports(int page, int size, String category);

    List<ResearchLibraryFileSnapshot> listResearchLibraryFiles(int limit);

    record MarketOverviewSnapshot(
            double topVolume24h,
            int activeStrategies,
            List<MarketTickerSnapshot> liveTickers
    ) {
    }

    record MarketTickerSnapshot(String symbol, String asset, double price, double change24h) {
    }

    record AcademyCourseSummarySnapshot(
            String courseId,
            String title,
            String level,
            int progress,
            int modules,
            double durationHours
    ) {
    }

    record AcademyCoursesSnapshot(List<AcademyCourseSummarySnapshot> items) {
    }

    record AcademyMetricsSnapshot(
            int activeStudents,
            int strategiesDeployed,
            double averagePerformancePercent,
            double academyRating
    ) {
    }

    record BlogPostSummarySnapshot(
            String postId,
            String title,
            String category,
            String excerpt,
            String authorName,
            LocalDateTime publishedAt,
            int readTimeMinutes
    ) {
    }

    record BlogPostsPageSnapshot(List<BlogPostSummarySnapshot> items, int page, int size, long totalElements) {
    }

    record ResearchReportSummarySnapshot(
            String reportId,
            String title,
            String category,
            String summary,
            LocalDateTime publishedAt,
            int readTimeMinutes
    ) {
    }

    record ResearchReportsPageSnapshot(List<ResearchReportSummarySnapshot> items, int page, int size, long totalElements) {
    }

    record ResearchLibraryFileSnapshot(String fileId, String title, String format, double sizeMb) {
    }
}
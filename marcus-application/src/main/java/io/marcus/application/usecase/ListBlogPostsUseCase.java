package io.marcus.application.usecase;

import io.marcus.domain.port.MarketingContentReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListBlogPostsUseCase {

    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 100;

    private final MarketingContentReadPort marketingContentReadPort;

    public MarketingContentReadPort.BlogPostsPageSnapshot execute(
            int page,
            int size,
            String query,
            String category
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        return marketingContentReadPort.listBlogPosts(
                normalizedPage,
                normalizedSize,
                normalizeOptional(query),
                normalizeOptional(category)
        );
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
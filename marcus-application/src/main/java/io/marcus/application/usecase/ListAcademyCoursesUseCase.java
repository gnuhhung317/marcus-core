package io.marcus.application.usecase;

import io.marcus.domain.port.MarketingContentReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListAcademyCoursesUseCase {

    private static final int DEFAULT_LIMIT = 12;
    private static final int MAX_LIMIT = 50;

    private final MarketingContentReadPort marketingContentReadPort;

    public MarketingContentReadPort.AcademyCoursesSnapshot execute(int limit) {
        int normalizedLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return marketingContentReadPort.listAcademyCourses(normalizedLimit);
    }
}
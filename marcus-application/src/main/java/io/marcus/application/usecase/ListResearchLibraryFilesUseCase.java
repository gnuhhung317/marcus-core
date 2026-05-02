package io.marcus.application.usecase;

import io.marcus.domain.port.MarketingContentReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListResearchLibraryFilesUseCase {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 50;

    private final MarketingContentReadPort marketingContentReadPort;

    public List<MarketingContentReadPort.ResearchLibraryFileSnapshot> execute(int limit) {
        int normalizedLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return marketingContentReadPort.listResearchLibraryFiles(normalizedLimit);
    }
}
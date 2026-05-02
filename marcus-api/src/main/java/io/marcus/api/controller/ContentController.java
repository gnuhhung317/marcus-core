package io.marcus.api.controller;

import io.marcus.application.usecase.ListBlogPostsUseCase;
import io.marcus.application.usecase.ListResearchLibraryFilesUseCase;
import io.marcus.application.usecase.ListResearchReportsUseCase;
import io.marcus.domain.port.MarketingContentReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/content", "/api/content", "/api/v1/content"})
@RequiredArgsConstructor
public class ContentController {

    private final ListBlogPostsUseCase listBlogPostsUseCase;
    private final ListResearchReportsUseCase listResearchReportsUseCase;
    private final ListResearchLibraryFilesUseCase listResearchLibraryFilesUseCase;

    @GetMapping("/blog/posts")
    public ResponseEntity<MarketingContentReadPort.BlogPostsPageSnapshot> listBlogPosts(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(listBlogPostsUseCase.execute(page, size, q, category));
    }

    @GetMapping("/research/reports")
    public ResponseEntity<MarketingContentReadPort.ResearchReportsPageSnapshot> listResearchReports(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(listResearchReportsUseCase.execute(page, size, category));
    }

    @GetMapping("/research/reports/library")
    public ResponseEntity<List<MarketingContentReadPort.ResearchLibraryFileSnapshot>> listResearchLibraryFiles(
            @RequestParam(required = false, defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(listResearchLibraryFilesUseCase.execute(limit));
    }
}
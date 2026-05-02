package io.marcus.api.controller;

import io.marcus.application.usecase.GetAcademyMetricsUseCase;
import io.marcus.application.usecase.ListAcademyCoursesUseCase;
import io.marcus.domain.port.MarketingContentReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/academy", "/api/academy", "/api/v1/academy"})
@RequiredArgsConstructor
public class AcademyController {

    private final ListAcademyCoursesUseCase listAcademyCoursesUseCase;
    private final GetAcademyMetricsUseCase getAcademyMetricsUseCase;

    @GetMapping("/courses")
    public ResponseEntity<MarketingContentReadPort.AcademyCoursesSnapshot> listAcademyCourses(
            @RequestParam(required = false, defaultValue = "12") int limit
    ) {
        return ResponseEntity.ok(listAcademyCoursesUseCase.execute(limit));
    }

    @GetMapping("/metrics")
    public ResponseEntity<MarketingContentReadPort.AcademyMetricsSnapshot> getAcademyMetrics() {
        return ResponseEntity.ok(getAcademyMetricsUseCase.execute());
    }
}
package io.marcus.api.controller.admin;

import io.marcus.application.dto.AdminDtos;
import io.marcus.application.usecase.AdminDashboardOverviewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin", "/api/admin", "/api/v1/admin"})
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardOverviewUseCase adminDashboardOverviewUseCase;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDtos.SystemOverview> getDashboardOverview() {
        return ResponseEntity.ok(adminDashboardOverviewUseCase.execute());
    }
}

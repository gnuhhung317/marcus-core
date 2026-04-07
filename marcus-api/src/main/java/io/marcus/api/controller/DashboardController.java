package io.marcus.api.controller;

import io.marcus.application.usecase.GetDashboardExchangeAllocationUseCase;
import io.marcus.application.usecase.GetDashboardOverviewUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/dashboard", "/api/dashboard", "/api/v1/dashboard"})
@RequiredArgsConstructor
public class DashboardController {

    private final GetDashboardOverviewUseCase getDashboardOverviewUseCase;
    private final GetDashboardExchangeAllocationUseCase getDashboardExchangeAllocationUseCase;

    @GetMapping("/overview")
    public ResponseEntity<TerminalReadPort.DashboardOverviewSnapshot> getOverview() {
        return ResponseEntity.ok(getDashboardOverviewUseCase.execute());
    }

    @GetMapping("/exchange-allocation")
    public ResponseEntity<List<TerminalReadPort.ExchangeAllocationSnapshot>> getExchangeAllocation() {
        return ResponseEntity.ok(getDashboardExchangeAllocationUseCase.execute());
    }
}

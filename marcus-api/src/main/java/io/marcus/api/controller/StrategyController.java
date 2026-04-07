package io.marcus.api.controller;

import io.marcus.application.usecase.GetStrategyDetailUseCase;
import io.marcus.application.usecase.GetStrategyMetricsUseCase;
import io.marcus.application.usecase.GetStrategyPerformanceSeriesUseCase;
import io.marcus.application.usecase.ListStrategyTradesUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/strategies", "/api/strategies", "/api/v1/strategies"})
@RequiredArgsConstructor
public class StrategyController {

    private final GetStrategyDetailUseCase getStrategyDetailUseCase;
    private final GetStrategyMetricsUseCase getStrategyMetricsUseCase;
    private final GetStrategyPerformanceSeriesUseCase getStrategyPerformanceSeriesUseCase;
    private final ListStrategyTradesUseCase listStrategyTradesUseCase;

    @GetMapping("/{strategyId}")
    public ResponseEntity<TerminalReadPort.StrategyDetailSnapshot> getStrategyDetail(
            @PathVariable String strategyId
    ) {
        return ResponseEntity.ok(getStrategyDetailUseCase.execute(strategyId));
    }

    @GetMapping("/{strategyId}/metrics")
    public ResponseEntity<TerminalReadPort.StrategyMetricsSnapshot> getStrategyMetrics(
            @PathVariable String strategyId,
            @RequestParam(required = false, defaultValue = "RAW") String feeMode
    ) {
        return ResponseEntity.ok(getStrategyMetricsUseCase.execute(strategyId, feeMode));
    }

    @GetMapping("/{strategyId}/performance-series")
    public ResponseEntity<List<TerminalReadPort.TimeSeriesPointSnapshot>> getStrategyPerformanceSeries(
            @PathVariable String strategyId,
            @RequestParam String range
    ) {
        return ResponseEntity.ok(getStrategyPerformanceSeriesUseCase.execute(strategyId, range));
    }

    @GetMapping("/{strategyId}/trades")
    public ResponseEntity<TerminalReadPort.TradeLogPageSnapshot> getStrategyTrades(
            @PathVariable String strategyId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String asset
    ) {
        return ResponseEntity.ok(listStrategyTradesUseCase.execute(strategyId, page, size, asset));
    }
}

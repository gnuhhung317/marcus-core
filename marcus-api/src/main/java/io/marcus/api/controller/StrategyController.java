package io.marcus.api.controller;

import io.marcus.application.usecase.FavoriteStrategyUseCase;
import io.marcus.application.usecase.ListStrategyTradesUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/strategies", "/api/strategies", "/api/v1/strategies"})
@RequiredArgsConstructor
public class StrategyController {

    private final FavoriteStrategyUseCase favoriteStrategyUseCase;
    private final ListStrategyTradesUseCase listStrategyTradesUseCase;

    @PostMapping("/{strategyId}/favorite")
    public ResponseEntity<TerminalReadPort.FavoriteStrategySnapshot> favoriteStrategy(
            @PathVariable String strategyId
    ) {
        return ResponseEntity.ok(favoriteStrategyUseCase.execute(strategyId));
    }

    @GetMapping("/{strategyId}/trades")
    public ResponseEntity<TerminalReadPort.TradeLogPageSnapshot> listStrategyTrades(
            @PathVariable String strategyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String asset
    ) {
        return ResponseEntity.ok(listStrategyTradesUseCase.execute(strategyId, page, size, asset));
    }
}

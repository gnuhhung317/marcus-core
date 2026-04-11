package io.marcus.api.controller;

import io.marcus.application.usecase.FavoriteStrategyUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/strategies", "/api/strategies", "/api/v1/strategies"})
@RequiredArgsConstructor
public class StrategyController {

    private final FavoriteStrategyUseCase favoriteStrategyUseCase;

    @PostMapping("/{strategyId}/favorite")
    public ResponseEntity<TerminalReadPort.FavoriteStrategySnapshot> favoriteStrategy(
            @PathVariable String strategyId
    ) {
        return ResponseEntity.ok(favoriteStrategyUseCase.execute(strategyId));
    }
}
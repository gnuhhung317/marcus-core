package io.marcus.api.controller;

import io.marcus.application.usecase.ListLeaderboardFeaturedUseCase;
import io.marcus.application.usecase.ListLeaderboardSpotlightsUseCase;
import io.marcus.application.usecase.ListLeaderboardStrategiesUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/leaderboard", "/api/leaderboard", "/api/v1/leaderboard"})
@RequiredArgsConstructor
public class LeaderboardController {

    private final ListLeaderboardStrategiesUseCase listLeaderboardStrategiesUseCase;
    private final ListLeaderboardFeaturedUseCase listLeaderboardFeaturedUseCase;
    private final ListLeaderboardSpotlightsUseCase listLeaderboardSpotlightsUseCase;

    @GetMapping("/strategies")
    public ResponseEntity<TerminalReadPort.LeaderboardStrategiesPageSnapshot> getLeaderboardStrategies(
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String asset,
            @RequestParam(required = false) String rankMetric,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                listLeaderboardStrategiesUseCase.execute(timeframe, market, asset, rankMetric, page, size)
        );
    }

    @GetMapping("/featured")
    public ResponseEntity<TerminalReadPort.LeaderboardFeaturedSnapshot> getLeaderboardFeatured() {
        return ResponseEntity.ok(listLeaderboardFeaturedUseCase.execute());
    }

    @GetMapping("/spotlights")
    public ResponseEntity<java.util.List<TerminalReadPort.StrategySpotlightSnapshot>> getLeaderboardSpotlights() {
        return ResponseEntity.ok(listLeaderboardSpotlightsUseCase.execute());
    }
}

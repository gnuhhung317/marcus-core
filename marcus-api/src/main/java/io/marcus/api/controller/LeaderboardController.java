package io.marcus.api.controller;

import io.marcus.application.usecase.ListLeaderboardFeaturedUseCase;
import io.marcus.application.usecase.ListLeaderboardSpotlightsUseCase;
import io.marcus.application.usecase.ListLeaderboardBotsUseCase;
import io.marcus.domain.port.BotDiscoveryReadPort;
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

    private final ListLeaderboardBotsUseCase listLeaderboardBotsUseCase;
    private final ListLeaderboardFeaturedUseCase listLeaderboardFeaturedUseCase;
    private final ListLeaderboardSpotlightsUseCase listLeaderboardSpotlightsUseCase;

    @GetMapping("/bots")
    public ResponseEntity<BotDiscoveryReadPort.LeaderboardBotsPageSnapshot> getLeaderboardBots(
            @RequestParam(value = "dataSource", required = false) String dataSource,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String asset,
            @RequestParam(required = false) String rankMetric,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                listLeaderboardBotsUseCase.execute(dataSource, market, asset, rankMetric, page, size)
        );
    }

    @GetMapping("/featured")
    public ResponseEntity<BotDiscoveryReadPort.LeaderboardFeaturedSnapshot> getLeaderboardFeatured() {
        return ResponseEntity.ok(listLeaderboardFeaturedUseCase.execute());
    }

    @GetMapping("/spotlights")
    public ResponseEntity<java.util.List<BotDiscoveryReadPort.BotSpotlightSnapshot>> getLeaderboardSpotlights() {
        return ResponseEntity.ok(listLeaderboardSpotlightsUseCase.execute());
    }
}

package io.marcus.api.controller;

import io.marcus.application.usecase.GetMarketOverviewUseCase;
import io.marcus.domain.port.MarketingContentReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/market", "/api/market", "/api/v1/market"})
@RequiredArgsConstructor
public class MarketController {

    private final GetMarketOverviewUseCase getMarketOverviewUseCase;

    @GetMapping("/overview")
    public ResponseEntity<MarketingContentReadPort.MarketOverviewSnapshot> getMarketOverview() {
        return ResponseEntity.ok(getMarketOverviewUseCase.execute());
    }
}
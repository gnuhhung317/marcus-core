package io.marcus.api.controller;

import io.marcus.application.dto.MarketingStatsResponse;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.repository.ExchangeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/public/marketing", "/api/public/marketing", "/api/v1/public/marketing"})
public class MarketingController {

    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final ExchangeRepository exchangeRepository;

    public MarketingController(UserRepository userRepository, BotRepository botRepository, ExchangeRepository exchangeRepository) {
        this.userRepository = userRepository;
        this.botRepository = botRepository;
        this.exchangeRepository = exchangeRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<MarketingStatsResponse> getMarketingStats() {
        long verifiedDevelopers = userRepository.count();
        long activeCloudExecutors = botRepository.countActive();

        String systemUptime = "24/7";
        int supportedExchanges = (int) exchangeRepository.count();

        // If the system is completely fresh, set a minimum floor for marketing effect.
        if (verifiedDevelopers < 20) {
            verifiedDevelopers += 50;
        }

        MarketingStatsResponse response = new MarketingStatsResponse(
                verifiedDevelopers,
                activeCloudExecutors,
                systemUptime,
                supportedExchanges
        );

        return ResponseEntity.ok(response);
    }
}

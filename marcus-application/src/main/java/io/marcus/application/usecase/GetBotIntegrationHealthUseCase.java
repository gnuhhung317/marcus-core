package io.marcus.application.usecase;

import io.marcus.domain.port.PortfolioReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBotIntegrationHealthUseCase {

    private final PortfolioReadPort portfolioReadPort;

    public PortfolioReadPort.BotIntegrationHealthSnapshot execute(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }

        return portfolioReadPort.getBotIntegrationHealth(botId.trim());
    }
}

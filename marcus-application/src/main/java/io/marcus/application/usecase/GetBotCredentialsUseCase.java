package io.marcus.application.usecase;

import io.marcus.domain.port.PortfolioReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBotCredentialsUseCase {

    private final PortfolioReadPort portfolioReadPort;

    public PortfolioReadPort.ApiKeySnapshot execute(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }

        return portfolioReadPort.getBotCredentials(botId.trim());
    }
}

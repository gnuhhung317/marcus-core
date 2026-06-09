package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ListBotTradesUseCase {

    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public BotDiscoveryReadPort.TradeLogPageSnapshot execute(String botId, int page, int size, String asset) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        String normalizedAsset = asset == null || asset.isBlank()
            ? null
            : asset.trim().toUpperCase(Locale.ROOT);

        return botDiscoveryReadPort.listBotTrades(botId.trim(), normalizedPage, normalizedSize, normalizedAsset);
    }
}

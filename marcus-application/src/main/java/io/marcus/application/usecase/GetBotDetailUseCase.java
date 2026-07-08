package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetBotDetailUseCase {

    private static final Set<String> SUPPORTED_SOURCES = Set.of("AUTO", "DRY_RUN", "HISTORICAL");

    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public BotDiscoveryReadPort.BotDetailSnapshot execute(String botId, String source) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }

        String normalizedSource = normalizeSource(source);
        return botDiscoveryReadPort.getBotDetail(botId.trim(), normalizedSource);
    }

    private String normalizeSource(String source) {
        String normalizedSource = source == null || source.isBlank()
                ? "AUTO"
                : source.trim().toUpperCase(Locale.ROOT);

        if (!SUPPORTED_SOURCES.contains(normalizedSource)) {
            throw new IllegalArgumentException("Unsupported source: " + source);
        }

        return normalizedSource;
    }
}

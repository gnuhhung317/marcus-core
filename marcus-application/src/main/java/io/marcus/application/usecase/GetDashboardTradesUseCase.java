package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GetDashboardTradesUseCase {

    private final IdentityService identityService;
    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public BotDiscoveryReadPort.TradeLogPageSnapshot execute(int page, int size, String asset) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        String normalizedAsset = asset == null || asset.isBlank()
                ? null
                : asset.trim().toUpperCase(Locale.ROOT);

        return botDiscoveryReadPort.listUserTrades(userId, normalizedPage, normalizedSize, normalizedAsset);
    }
}

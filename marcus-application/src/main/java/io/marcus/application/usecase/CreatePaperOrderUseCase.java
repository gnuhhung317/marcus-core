package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreatePaperOrderUseCase {

    private static final Set<String> SUPPORTED_ORDER_TYPES = Set.of("MARKET", "LIMIT");
    private static final Set<String> SUPPORTED_SIDES = Set.of("BUY", "SELL");

    private final IdentityService identityService;
    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.PaperOrderSnapshot execute(
            String assetPair,
            String orderType,
            String side,
            double quantity,
            Double limitPrice
    ) {
        String userId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (assetPair == null || assetPair.isBlank()) {
            throw new IllegalArgumentException("Asset pair is required");
        }
        if (quantity <= 0.0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        String normalizedOrderType = orderType == null ? "" : orderType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ORDER_TYPES.contains(normalizedOrderType)) {
            throw new IllegalArgumentException("Unsupported order type: " + orderType);
        }

        String normalizedSide = side == null ? "" : side.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_SIDES.contains(normalizedSide)) {
            throw new IllegalArgumentException("Unsupported side: " + side);
        }

        if ("MARKET".equals(normalizedOrderType) && limitPrice != null) {
            throw new IllegalArgumentException("limitPrice must be omitted for MARKET orders");
        }
        if ("LIMIT".equals(normalizedOrderType) && (limitPrice == null || limitPrice <= 0.0)) {
            throw new IllegalArgumentException("limitPrice must be provided and greater than 0 for LIMIT orders");
        }

        TerminalReadPort.PaperOrderCreateSnapshot request = new TerminalReadPort.PaperOrderCreateSnapshot(
                assetPair.trim().toUpperCase(Locale.ROOT),
                normalizedOrderType,
                normalizedSide,
                quantity,
                limitPrice
        );
        return terminalReadPort.createPaperOrder(userId, request);
    }
}

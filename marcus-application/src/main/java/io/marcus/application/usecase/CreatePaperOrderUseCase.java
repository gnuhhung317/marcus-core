package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreatePaperOrderUseCase {

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

        TerminalReadPort.PaperOrderCreateSnapshot request = new TerminalReadPort.PaperOrderCreateSnapshot(
                assetPair,
                orderType,
                side,
                quantity,
                limitPrice
        );
        return terminalReadPort.createPaperOrder(userId, request);
    }
}

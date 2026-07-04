package io.marcus.api.controller;

import io.marcus.application.dto.CaptureSignalRequest;
import io.marcus.application.usecase.CaptureSignalUseCase;
import io.marcus.application.usecase.ListSignalsUseCase;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.infrastructure.cache.RedisCacheInvalidator;
import io.marcus.infrastructure.security.RequireBotSignature;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/signals", "/api/signals", "/api/v1/signals"})
@RequiredArgsConstructor
public class SignalController {

    private final CaptureSignalUseCase captureSignalUseCase;
    private final ListSignalsUseCase listSignalsUseCase;

    @Autowired(required = false)
    private RedisCacheInvalidator cacheInvalidator;

    @RequireBotSignature
    @PostMapping
    public ResponseEntity<Void> captureSignal(
            @Valid @RequestBody CaptureSignalRequest request
    ) {
        captureSignalUseCase.execute(request);
        if (cacheInvalidator != null) {
            cacheInvalidator.evictSignalDerivedCatalog(request.botId());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<PortfolioReadPort.SignalItemSnapshot>> listSignals(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "50") int limit,
            @RequestParam(required = false) String botId,
            @RequestParam(required = false) String signalId
    ) {
        return ResponseEntity.ok(listSignalsUseCase.execute(status, limit, botId, signalId));
    }
}

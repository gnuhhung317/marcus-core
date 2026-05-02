package io.marcus.api.controller;

import io.marcus.application.usecase.CaptureSignalUseCase;
import io.marcus.application.usecase.ListSignalsUseCase;
import io.marcus.domain.model.Signal;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.infrastructure.security.RequireBotSignature;
import lombok.RequiredArgsConstructor;
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

    @RequireBotSignature
    @PostMapping
    public ResponseEntity<Void> captureSignal(
            @RequestBody Signal signal
    ) {
        captureSignalUseCase.execute(signal);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<TerminalReadPort.SignalItemSnapshot>> listSignals(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(listSignalsUseCase.execute(status, limit));
    }
}

package io.marcus.api.controller;

import io.marcus.application.usecase.CaptureSignalUseCase;
import io.marcus.domain.model.Signal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.marcus.infrastructure.security.RequireBotSignature;

@RestController
@RequestMapping({"/signal", "/signals", "/api/v1/signal", "/api/v1/signals"})
@RequiredArgsConstructor
public class SignalController {

    private final CaptureSignalUseCase captureSignalUseCase;

    @RequireBotSignature
    @PostMapping
    public ResponseEntity<Void> captureSignal(
            @RequestBody Signal signal
    ) {

        //FIXME: validate later
        captureSignalUseCase.execute(signal);
        return ResponseEntity.ok().build();
    }
}

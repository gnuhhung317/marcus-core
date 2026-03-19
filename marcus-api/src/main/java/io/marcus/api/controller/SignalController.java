package io.marcus.api.controller;

import io.marcus.application.usecase.CaptureSignalUseCase;
import io.marcus.domain.model.Signal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;


@RestController("/signal")
@RequiredArgsConstructor
public class SignalController {

    private final CaptureSignalUseCase captureSignalUseCase;

    @PostMapping
    public ResponseEntity<Void> captureSignal(
            @RequestBody Signal signal
            ) {

        //FIXME: validate later
        captureSignalUseCase.execute(signal);
        return ResponseEntity.ok().build();
    }
}

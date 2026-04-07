package io.marcus.api.controller;

import io.marcus.application.usecase.GetPaperSessionSummaryUseCase;
import io.marcus.application.usecase.ListPaperSignalsUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/paper", "/api/paper", "/api/v1/paper"})
@RequiredArgsConstructor
public class PaperController {

    private final GetPaperSessionSummaryUseCase getPaperSessionSummaryUseCase;
    private final ListPaperSignalsUseCase listPaperSignalsUseCase;

    @GetMapping("/session")
    public ResponseEntity<TerminalReadPort.PaperSessionSummarySnapshot> getPaperSessionSummary() {
        return ResponseEntity.ok(getPaperSessionSummaryUseCase.execute());
    }

    @GetMapping("/signals")
    public ResponseEntity<List<TerminalReadPort.PaperSignalSnapshot>> listPaperSignals(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(listPaperSignalsUseCase.execute(status, limit));
    }
}

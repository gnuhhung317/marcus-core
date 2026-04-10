package io.marcus.api.controller;

import io.marcus.application.usecase.CreatePaperOrderUseCase;
import io.marcus.application.usecase.GetPaperSessionSummaryUseCase;
import io.marcus.application.usecase.ListPaperSignalsUseCase;
import io.marcus.application.usecase.ListPaperExecutionLogsUseCase;
import io.marcus.application.usecase.PausePaperSessionUseCase;
import io.marcus.application.usecase.ResumePaperSessionUseCase;
import io.marcus.domain.port.TerminalReadPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/paper", "/api/paper", "/api/v1/paper"})
@RequiredArgsConstructor
public class PaperController {

    private final GetPaperSessionSummaryUseCase getPaperSessionSummaryUseCase;
    private final ListPaperExecutionLogsUseCase listPaperExecutionLogsUseCase;
    private final ListPaperSignalsUseCase listPaperSignalsUseCase;
    private final CreatePaperOrderUseCase createPaperOrderUseCase;
    private final PausePaperSessionUseCase pausePaperSessionUseCase;
    private final ResumePaperSessionUseCase resumePaperSessionUseCase;

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

    @GetMapping("/logs")
    public ResponseEntity<TerminalReadPort.PaperExecutionLogPageSnapshot> listPaperLogs(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(listPaperExecutionLogsUseCase.execute(cursor, limit));
    }

    @PostMapping("/orders")
    public ResponseEntity<TerminalReadPort.PaperOrderSnapshot> createPaperOrder(
            @Valid @RequestBody CreatePaperOrderRequest request
    ) {
        TerminalReadPort.PaperOrderSnapshot result = createPaperOrderUseCase.execute(
                request.assetPair(),
                request.orderType(),
                request.side(),
                request.quantity(),
                request.limitPrice()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/session/pause")
    public ResponseEntity<TerminalReadPort.PaperSessionStateSnapshot> pauseSession() {
        return ResponseEntity.ok(pausePaperSessionUseCase.execute());
    }

    @PostMapping("/session/resume")
    public ResponseEntity<TerminalReadPort.PaperSessionStateSnapshot> resumeSession() {
        return ResponseEntity.ok(resumePaperSessionUseCase.execute());
    }

    public record CreatePaperOrderRequest(
            @NotBlank(message = "assetPair is required")
            String assetPair,

            @NotBlank(message = "orderType is required")
            @Pattern(regexp = "MARKET|LIMIT", message = "orderType must be MARKET or LIMIT")
            String orderType,

            @NotBlank(message = "side is required")
            @Pattern(regexp = "BUY|SELL", message = "side must be BUY or SELL")
            String side,

            @Positive(message = "quantity must be greater than 0")
            double quantity,

            Double limitPrice
    ) {
    }
}

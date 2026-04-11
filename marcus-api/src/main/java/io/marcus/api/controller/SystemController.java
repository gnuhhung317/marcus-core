package io.marcus.api.controller;

import io.marcus.application.usecase.GetSystemConnectivityHealthUseCase;
import io.marcus.application.usecase.ListSystemExecutionLogsUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/system", "/api/system", "/api/v1/system"})
@RequiredArgsConstructor
public class SystemController {

    private final GetSystemConnectivityHealthUseCase getSystemConnectivityHealthUseCase;
    private final ListSystemExecutionLogsUseCase listSystemExecutionLogsUseCase;

    @GetMapping("/connectivity")
    public ResponseEntity<TerminalReadPort.ConnectivityHealthSnapshot> getSystemConnectivity() {
        return ResponseEntity.ok(getSystemConnectivityHealthUseCase.execute());
    }

    @GetMapping("/execution-logs")
    public ResponseEntity<TerminalReadPort.ExecutionLogPageSnapshot> listExecutionLogs(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(listSystemExecutionLogsUseCase.execute(cursor, limit));
    }
}

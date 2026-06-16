package io.marcus.api.controller.admin;

import io.marcus.application.usecase.GetSystemConnectivityHealthUseCase;
import io.marcus.application.usecase.ListSystemExecutionLogsUseCase;
import io.marcus.domain.port.PortfolioReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin/system", "/api/admin/system", "/api/v1/admin/system"})
@RequiredArgsConstructor
public class AdminSystemController {

    private final GetSystemConnectivityHealthUseCase getSystemConnectivityHealthUseCase;
    private final ListSystemExecutionLogsUseCase listSystemExecutionLogsUseCase;

    @GetMapping("/connectivity")
    public ResponseEntity<PortfolioReadPort.ConnectivityHealthSnapshot> getSystemConnectivity() {
        return ResponseEntity.ok(getSystemConnectivityHealthUseCase.execute());
    }

    @GetMapping("/execution-logs")
    public ResponseEntity<PortfolioReadPort.ExecutionLogPageSnapshot> listExecutionLogs(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(listSystemExecutionLogsUseCase.execute(cursor, limit));
    }
}

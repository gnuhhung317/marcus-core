package io.marcus.api.controller;

import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.usecase.ListPublicBotsUseCase;
import io.marcus.application.usecase.RegisterBotUseCase;
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bots")
@RequiredArgsConstructor
public class BotController {
    private final RegisterBotUseCase registerBotUseCase;
    private final ListPublicBotsUseCase listPublicBotsUseCase;

    @PostMapping("/register")
    public BotRegistrationResult registerBot(@RequestBody RegisterBotRequest botRequest) {
        return registerBotUseCase.execute(botRequest);
    }

    @GetMapping
    public ResponseEntity<TerminalReadPort.BotDiscoveryPageSnapshot> listPublicBots(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String asset,
            @RequestParam(required = false) String risk,
            @RequestParam(required = false, defaultValue = "-return") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(listPublicBotsUseCase.execute(q, asset, risk, sort, page, size));
    }
}

package io.marcus.api.controller;

import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.usecase.ListDeveloperBotsUseCase;
import io.marcus.application.usecase.ListPublicBotsUseCase;
import io.marcus.application.usecase.RegisterBotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/bots", "/api/bots", "/api/v1/bots"})
@RequiredArgsConstructor
public class BotController {

    private final RegisterBotUseCase registerBotUseCase;
    private final ListPublicBotsUseCase listPublicBotsUseCase;
    private final ListDeveloperBotsUseCase listDeveloperBotsUseCase;

    @GetMapping
    public ResponseEntity<List<BotSummaryResult>> getPublicBots() {
        return ResponseEntity.ok(listPublicBotsUseCase.execute());
    }

    @GetMapping("/my-bots")
    public ResponseEntity<List<BotSummaryResult>> getMyBots() {
        return ResponseEntity.ok(listDeveloperBotsUseCase.execute());
    }

    @PostMapping({"", "/register"})
    public ResponseEntity<BotRegistrationResult> registerBot(@RequestBody RegisterBotRequest botRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registerBotUseCase.execute(botRequest));
    }
}

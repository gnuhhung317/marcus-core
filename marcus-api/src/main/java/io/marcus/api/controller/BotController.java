package io.marcus.api.controller;

import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.usecase.RegisterBotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/bots")
@RequiredArgsConstructor
public class BotController {
    private final RegisterBotUseCase registerBotUseCase;

    @PostMapping("/register")
    public BotRegistrationResult registerBot(RegisterBotRequest botRequest) {
        return registerBotUseCase.execute(botRequest);
    }
}

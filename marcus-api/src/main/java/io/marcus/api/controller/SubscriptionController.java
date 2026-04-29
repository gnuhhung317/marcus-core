package io.marcus.api.controller;

import io.marcus.application.dto.BotSubscriptionResult;
import io.marcus.application.usecase.SubscribeToBotUseCase;
import io.marcus.application.usecase.UnsubscribeFromBotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/subscriptions", "/api/v1/subscriptions"})
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscribeToBotUseCase subscribeToBotUseCase;
    private final UnsubscribeFromBotUseCase unsubscribeFromBotUseCase;
    private final io.marcus.application.usecase.ConnectExecutorToSubscriptionUseCase connectExecutorToSubscriptionUseCase;
    private final io.marcus.application.usecase.ListActiveSubscriptionsForBotUseCase listActiveSubscriptionsForBotUseCase;

    @PostMapping("/{botId}")
    public ResponseEntity<BotSubscriptionResult> subscribe(@PathVariable String botId) {
        return ResponseEntity.ok(subscribeToBotUseCase.execute(botId));
    }

    @DeleteMapping("/{botId}")
    public ResponseEntity<BotSubscriptionResult> unsubscribe(@PathVariable String botId) {
        return ResponseEntity.ok(unsubscribeFromBotUseCase.execute(botId));
    }

    public static record ConnectRequest(String wsToken) {

    }

    @PostMapping("/{botId}/connect")
    public ResponseEntity<BotSubscriptionResult> connectExecutor(@PathVariable String botId, @RequestBody ConnectRequest request) {
        return connectExecutorToSubscriptionUseCase.execute(botId, request.wsToken())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(403).build());
    }

    @GetMapping("/{botId}/active")
    public ResponseEntity<List<BotSubscriptionResult>> listActiveForBot(@PathVariable String botId) {
        return ResponseEntity.ok(listActiveSubscriptionsForBotUseCase.execute(botId));
    }
}

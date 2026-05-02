package io.marcus.api.controller;

import io.marcus.application.dto.BotSubscriptionResult;
import io.marcus.application.dto.MySubscriptionsResult;
import io.marcus.application.dto.SubscribeBotResult;
import io.marcus.application.usecase.SubscribeToBotUseCase;
import io.marcus.application.usecase.UnsubscribeFromBotUseCase;
import io.marcus.application.usecase.SubscribeBotUseCase;
import io.marcus.application.usecase.ListMySubscriptionsUseCase;
import io.marcus.application.usecase.ConnectExecutorToSubscriptionUseCase;
import io.marcus.application.usecase.ListActiveSubscriptionsForBotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/subscriptions", "/api/subscriptions", "/api/v1/subscriptions"})
@RequiredArgsConstructor
public class SubscriptionController {

    // User-focused use cases
    private final SubscribeBotUseCase subscribeBotUseCase;
    private final ListMySubscriptionsUseCase listMySubscriptionsUseCase;
    
    // Bot/Admin-focused use cases
    private final SubscribeToBotUseCase subscribeToBotUseCase;
    private final UnsubscribeFromBotUseCase unsubscribeFromBotUseCase;
    private final ConnectExecutorToSubscriptionUseCase connectExecutorToSubscriptionUseCase;
    private final ListActiveSubscriptionsForBotUseCase listActiveSubscriptionsForBotUseCase;

    // User endpoints
    @PostMapping("/{botId}")
    public ResponseEntity<SubscribeBotResult> subscribeBot(@PathVariable String botId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscribeBotUseCase.execute(botId));
    }

    @GetMapping({"", "/my-subscriptions"})
    public ResponseEntity<MySubscriptionsResult> getMySubscriptions() {
        return ResponseEntity.ok(listMySubscriptionsUseCase.execute());
    }

    // Bot/Admin endpoints (alternative naming)
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

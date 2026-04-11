package io.marcus.api.controller;

import io.marcus.application.dto.MySubscriptionsResult;
import io.marcus.application.dto.SubscribeBotResult;
import io.marcus.application.usecase.ListMySubscriptionsUseCase;
import io.marcus.application.usecase.SubscribeBotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/subscriptions", "/api/subscriptions", "/api/v1/subscriptions"})
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscribeBotUseCase subscribeBotUseCase;
    private final ListMySubscriptionsUseCase listMySubscriptionsUseCase;

    @PostMapping("/{botId}")
    public ResponseEntity<SubscribeBotResult> subscribeBot(@PathVariable String botId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscribeBotUseCase.execute(botId));
    }

    @GetMapping({"", "/my-subscriptions"})
    public ResponseEntity<MySubscriptionsResult> getMySubscriptions() {
        return ResponseEntity.ok(listMySubscriptionsUseCase.execute());
    }
}

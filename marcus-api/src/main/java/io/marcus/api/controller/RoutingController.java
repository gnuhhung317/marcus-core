package io.marcus.api.controller;

import io.marcus.application.dto.RemoveBotSubscriberRequest;
import io.marcus.application.dto.RemoveUserSessionRequest;
import io.marcus.application.dto.UpsertBotSubscriberRequest;
import io.marcus.application.dto.UpsertUserSessionRequest;
import io.marcus.application.usecase.RemoveBotSubscriberUseCase;
import io.marcus.application.usecase.RemoveUserSessionUseCase;
import io.marcus.application.usecase.UpsertBotSubscriberUseCase;
import io.marcus.application.usecase.UpsertUserSessionUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping({"/routing", "/api/v1/routing"})
public class RoutingController {

    private final UpsertUserSessionUseCase upsertUserSessionUseCase;
    private final UpsertBotSubscriberUseCase upsertBotSubscriberUseCase;
    private final RemoveUserSessionUseCase removeUserSessionUseCase;
    private final RemoveBotSubscriberUseCase removeBotSubscriberUseCase;

    public RoutingController(
            UpsertUserSessionUseCase upsertUserSessionUseCase,
            UpsertBotSubscriberUseCase upsertBotSubscriberUseCase,
            RemoveUserSessionUseCase removeUserSessionUseCase,
            RemoveBotSubscriberUseCase removeBotSubscriberUseCase
    ) {
        this.upsertUserSessionUseCase = upsertUserSessionUseCase;
        this.upsertBotSubscriberUseCase = upsertBotSubscriberUseCase;
        this.removeUserSessionUseCase = removeUserSessionUseCase;
        this.removeBotSubscriberUseCase = removeBotSubscriberUseCase;
    }

    @PostMapping("/sessions")
    public ResponseEntity<Void> upsertUserSession(@RequestBody UpsertUserSessionRequest request) {
        upsertUserSessionUseCase.execute(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/subscribers")
    public ResponseEntity<Void> upsertBotSubscriber(@RequestBody UpsertBotSubscriberRequest request) {
        upsertBotSubscriberUseCase.execute(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<Void> removeUserSession(
            @RequestParam String userId,
            @RequestParam String sessionId
    ) {
        removeUserSessionUseCase.execute(new RemoveUserSessionRequest(userId, sessionId));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/subscribers")
    public ResponseEntity<Void> removeBotSubscriber(
            @RequestParam String botId,
            @RequestParam String userId
    ) {
        removeBotSubscriberUseCase.execute(new RemoveBotSubscriberRequest(botId, userId));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
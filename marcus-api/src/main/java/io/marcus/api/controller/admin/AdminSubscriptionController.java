package io.marcus.api.controller.admin;

import io.marcus.application.dto.AdminDtos;
import io.marcus.application.usecase.AdminForceCancelSubscriptionUseCase;
import io.marcus.infrastructure.cache.RedisCacheInvalidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin", "/api/admin", "/api/v1/admin"})
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final AdminForceCancelSubscriptionUseCase adminForceCancelSubscriptionUseCase;

    @Autowired(required = false)
    private RedisCacheInvalidator cacheInvalidator;

    @PatchMapping("/subscriptions/{userSubscriptionId}/force-cancel")
    public ResponseEntity<AdminDtos.BotSubscriberRow> forceCancelSubscription(
            @PathVariable String userSubscriptionId,
            @RequestBody AdminDtos.ForceCancelSubscriptionRequest request
    ) {
        AdminDtos.BotSubscriberRow row = adminForceCancelSubscriptionUseCase.execute(userSubscriptionId, request);
        if (cacheInvalidator != null) {
            cacheInvalidator.evictSubscriptionCatalog(null);
        }
        return ResponseEntity.ok(row);
    }
}

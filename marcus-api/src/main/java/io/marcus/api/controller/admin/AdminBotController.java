package io.marcus.api.controller.admin;

import io.marcus.application.dto.AdminDtos;
import io.marcus.application.usecase.AdminGetBotDetailUseCase;
import io.marcus.application.usecase.AdminListBotSubscribersUseCase;
import io.marcus.application.usecase.AdminListBotsUseCase;
import io.marcus.application.usecase.AdminUpdateBotStatusUseCase;
import io.marcus.application.usecase.ListSignalsUseCase;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.cache.RedisCacheInvalidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/admin", "/api/admin", "/api/v1/admin"})
@RequiredArgsConstructor
public class AdminBotController {

    private final AdminListBotsUseCase adminListBotsUseCase;
    private final AdminGetBotDetailUseCase adminGetBotDetailUseCase;
    private final AdminUpdateBotStatusUseCase adminUpdateBotStatusUseCase;
    private final AdminListBotSubscribersUseCase adminListBotSubscribersUseCase;
    private final ListSignalsUseCase listSignalsUseCase;

    @Autowired(required = false)
    private RedisCacheInvalidator cacheInvalidator;

    @GetMapping("/bots")
    public ResponseEntity<AdminDtos.PageResponse<AdminDtos.BotRow>> listBots(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BotStatus status,
            @RequestParam(required = false) String developerId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminListBotsUseCase.execute(query, status, developerId, page, size));
    }

    @GetMapping("/bots/{botId}")
    public ResponseEntity<AdminDtos.BotDetail> getBotDetail(@PathVariable String botId) {
        return ResponseEntity.ok(adminGetBotDetailUseCase.execute(botId));
    }

    @PatchMapping("/bots/{botId}/status")
    public ResponseEntity<AdminDtos.BotRow> updateBotStatus(
            @PathVariable String botId,
            @RequestBody AdminDtos.UpdateBotStatusRequest request
    ) {
        AdminDtos.BotRow row = adminUpdateBotStatusUseCase.execute(botId, request);
        if (cacheInvalidator != null) {
            cacheInvalidator.evictBotCatalog(botId);
        }
        return ResponseEntity.ok(row);
    }

    @GetMapping("/bots/{botId}/signals")
    public ResponseEntity<List<PortfolioReadPort.SignalItemSnapshot>> listBotSignals(
            @PathVariable String botId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(listSignalsUseCase.execute(status, limit, botId, null));
    }

    @GetMapping("/bots/{botId}/subscribers")
    public ResponseEntity<AdminDtos.PageResponse<AdminDtos.BotSubscriberRow>> listBotSubscribers(
            @PathVariable String botId,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminListBotSubscribersUseCase.execute(botId, status, page, size));
    }
}

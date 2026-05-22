package io.marcus.application.dto;

import io.marcus.domain.vo.BotStatus;

public record UpdateBotStatusRequest(
        BotStatus status
) {
}

package io.marcus.domain.port;

import io.marcus.domain.model.Bot;
import io.marcus.domain.model.PagedResult;
import io.marcus.domain.vo.BotStatus;

import java.util.Optional;

public interface AdminBotPort {
    PagedResult<Bot> search(String query, BotStatus status, String developerId, int page, int size);

    Optional<Bot> findByBotId(String botId);

    Bot save(Bot bot);

    long countAll();

    long countByStatus(BotStatus status);
}

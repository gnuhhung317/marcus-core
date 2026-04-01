package io.marcus.domain.repository;

import io.marcus.domain.model.Bot;

import java.util.List;
import java.util.Optional;

public interface BotRepository {

    Bot save(Bot bot);

    Optional<Bot> findByBotId(String botId);

    List<Bot> findAllActive();

    List<Bot> findAllByDeveloperId(String developerId);

    Optional<String> findSecretByBotId(String botId);

    Optional<String> findSecretByApiKey(String apiKey);
}

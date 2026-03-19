package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.Bot;
import io.marcus.domain.repository.BotRepository;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.mapper.BotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaBotRepositoryImpl implements BotRepository {

    private final SpringDataBotRepository springDataBotRepository;
    private final BotMapper botMapper;

    @Override
    public Optional<String> findSecretByBotId(String botId) {
        return springDataBotRepository.findByBotId(botId)
                .map(BotEntity::getSecretKey);
    }

    @Override
    public Optional<String> findSecretByApiKey(String apiKey) {
        return springDataBotRepository.findByApiKey(apiKey)
                .map(BotEntity::getSecretKey);
    }

    @Override
    public Bot save(Bot bot) {
        return botMapper.toDomain(springDataBotRepository.save(botMapper.toEntity(bot)));
    }

    @Override
    public Optional<Bot> findByBotId(String botId) {
        return springDataBotRepository.findByBotId(botId).map(botMapper::toDomain);
    }
}

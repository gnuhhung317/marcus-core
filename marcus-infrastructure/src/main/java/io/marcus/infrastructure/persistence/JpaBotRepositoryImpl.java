package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.Bot;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.vo.BotStatus;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.ExchangeEntity;
import io.marcus.infrastructure.persistence.mapper.BotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaBotRepositoryImpl implements BotRepository {

    private final SpringDataBotRepository springDataBotRepository;
    private final SpringDataExchangeRepository springDataExchangeRepository;
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
        BotEntity entity = botMapper.toEntity(bot);
        springDataBotRepository.findByBotId(bot.getBotId())
                .ifPresent(existing -> entity.setId(existing.getId()));

        String lookupExchangeId = bot.getExchangeId() != null ? bot.getExchangeId().toLowerCase() : null;
        ExchangeEntity exchange = springDataExchangeRepository.findByExchangeId(lookupExchangeId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange not found: " + bot.getExchangeId()));
        entity.setExchange(exchange);

        return botMapper.toDomain(springDataBotRepository.save(entity));
    }

    @Override
    public Optional<Bot> findByBotId(String botId) {
        return springDataBotRepository.findByBotId(botId).map(botMapper::toDomain);
    }

    @Override
    public List<Bot> findAllActive() {
        return springDataBotRepository.findByStatus(BotStatus.ACTIVE)
                .stream()
                .map(botMapper::toDomain)
                .toList();
    }

    @Override
    public long countActive() {
        return springDataBotRepository.countByStatus(BotStatus.ACTIVE);
    }

    @Override
    public List<Bot> findAllByDeveloperId(String developerId) {
        return springDataBotRepository.findByDeveloperId(developerId)
                .stream()
                .filter(bot -> bot.getStatus() != BotStatus.DELETED)
                .map(botMapper::toDomain)
                .toList();
    }
}

package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.Bot;
import io.marcus.domain.model.PagedResult;
import io.marcus.domain.port.AdminBotPort;
import io.marcus.domain.vo.BotStatus;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.mapper.BotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaAdminBotAdapter implements AdminBotPort {

    private final SpringDataBotRepository springDataBotRepository;
    private final BotMapper botMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResult<Bot> search(String query, BotStatus status, String developerId, int page, int size) {
        Page<BotEntity> result = springDataBotRepository.searchAdminBots(
                normalize(query),
                status,
                normalize(developerId),
                PageRequest.of(Math.max(0, page), Math.max(1, size))
        );
        return new PagedResult<>(
                result.getContent().stream().map(botMapper::toDomain).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bot> findByBotId(String botId) {
        if (botId == null || botId.isBlank()) {
            return Optional.empty();
        }
        return springDataBotRepository.findByBotId(botId.trim()).map(botMapper::toDomain);
    }

    @Override
    @Transactional
    public Bot save(Bot bot) {
        BotEntity entity = botMapper.toEntity(bot);
        springDataBotRepository.findByBotId(bot.getBotId())
                .ifPresent(existing -> entity.setId(existing.getId()));
        return botMapper.toDomain(springDataBotRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return springDataBotRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(BotStatus status) {
        return springDataBotRepository.countByStatus(status);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}

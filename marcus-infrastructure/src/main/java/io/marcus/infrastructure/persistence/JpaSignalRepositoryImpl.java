package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.Signal;
import io.marcus.domain.repository.SignalRepository;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.mapper.SignalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaSignalRepositoryImpl implements SignalRepository {

    private final SpringDataSignalRepository springDataSignalRepository;
    private final SignalMapper signalMapper;

    @Override
    public void save(Signal signal) {
        SignalEntity entity = signalMapper.toEntity(signal);
        springDataSignalRepository.save(entity);
    }

    @Override
    public boolean existsBySignalId(String signalId) {
        return springDataSignalRepository.findBySignalId(signalId).isPresent();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateStatus(String signalId, io.marcus.domain.vo.SignalStatus status) {
        springDataSignalRepository.findBySignalId(signalId).ifPresent(entity -> {
            entity.setStatus(status);
            // Entity becomes managed and automatically updated by Hibernate/SpringData on transaction commit
        });
    }

    @Override
    public java.util.List<Signal> findByBotId(String botId, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        return springDataSignalRepository.findByBotIdOrderByGeneratedTimestampDesc(
                botId,
                org.springframework.data.domain.PageRequest.of(0, normalizedLimit)
        ).stream().map(signalMapper::toDomain).toList();
    }

    @Override
    public java.util.Optional<Signal> findBySignalId(String signalId) {
        return springDataSignalRepository.findBySignalId(signalId).map(signalMapper::toDomain);
    }
}

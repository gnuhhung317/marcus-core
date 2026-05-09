package io.marcus.infrastructure.persistence;

import io.marcus.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaExchangeRepositoryImpl implements ExchangeRepository {

    private final SpringDataExchangeRepository springDataExchangeRepository;

    @Override
    public long count() {
        return springDataExchangeRepository.count();
    }
}

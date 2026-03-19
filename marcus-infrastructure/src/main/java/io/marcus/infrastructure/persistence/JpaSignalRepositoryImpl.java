//package io.marcus.infrastructure.persistence;
//
//import io.marcus.domain.model.Signal;
//import io.marcus.domain.repository.SignalRepository;
//import io.marcus.infrastructure.persistence.entity.SignalEntity;
//import io.marcus.infrastructure.persistence.mapper.SignalMapper;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Primary;
//import org.springframework.stereotype.Repository;
//
//@Repository
////@Primary
//@RequiredArgsConstructor
//public class JpaSignalRepositoryImpl implements SignalRepository {
//
//    private final SpringDataSignalRepository springDataSignalRepository;
//    private final SignalMapper signalMapper;
//
//    @Override
//    public void publish(Signal signal) {
//        SignalEntity entity = signalMapper.toEntity(signal);
//        springDataSignalRepository.save(entity);
//    }
//}

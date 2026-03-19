package io.marcus.application.usecase;

import io.marcus.domain.model.Signal;
import io.marcus.domain.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CaptureSignalUseCase {
    private final SignalRepository signalRepository;

    public void execute(Signal signal){

        signalRepository.publish(signal);
    }

}

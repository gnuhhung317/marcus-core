package io.marcus.domain.repository;

import io.marcus.domain.model.Signal;

public interface SignalRepository {
    void publish(Signal signal);
}

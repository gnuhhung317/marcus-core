package io.marcus.domain.port;

import io.marcus.domain.model.Signal;

public interface SignalPublisherPort {

    void publish(Signal signal);
}

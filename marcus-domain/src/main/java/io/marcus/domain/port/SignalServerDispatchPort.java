package io.marcus.domain.port;

import io.marcus.domain.model.Signal;

import java.util.Set;

public interface SignalServerDispatchPort {

    void dispatchToServers(Signal signal, Set<String> serverIds);
}
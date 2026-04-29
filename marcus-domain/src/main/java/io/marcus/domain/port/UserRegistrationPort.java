package io.marcus.domain.port;

import io.marcus.domain.model.User;

public interface UserRegistrationPort {

    User save(User user);
}

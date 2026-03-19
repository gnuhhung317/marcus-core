package io.marcus.domain.port;

public interface PasswordHashPort {

    boolean matches(String rawPassword, String encodedPassword);
}

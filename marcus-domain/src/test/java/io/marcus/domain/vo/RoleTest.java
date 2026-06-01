package io.marcus.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTest {

    @Test
    void shouldMapLegacyUserRoleToTrader() {
        assertEquals(Role.TRADER, Role.fromValue("USER"));
    }

    @Test
    void shouldNormalizeRoleNameBeforeMapping() {
        assertEquals(Role.TRADER, Role.fromValue(" trader "));
    }
}
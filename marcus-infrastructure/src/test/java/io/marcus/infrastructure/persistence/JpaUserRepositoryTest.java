package io.marcus.infrastructure.persistence;

import io.marcus.domain.vo.Role;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserRepositoryTest {

    @Mock
    private SpringDataUserRepository springDataUserRepository;

    private JpaUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaUserRepository(springDataUserRepository);
    }

    @Test
    void shouldTreatLegacyUserRoleAsTraderWhenLookingUpByIdAndRole() {
        UserEntity userEntity = UserEntity.builder()
                .userId("usr_1")
                .role(Role.TRADER)
                .build();

        when(springDataUserRepository.findByUserId("usr_1")).thenReturn(Optional.of(userEntity));

        assertThat(repository.existsByIdAndRole("usr_1", Role.TRADER)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenRoleDoesNotMatch() {
        UserEntity userEntity = UserEntity.builder()
                .userId("usr_1")
                .role(Role.DEVELOPER)
                .build();

        when(springDataUserRepository.findByUserId("usr_1")).thenReturn(Optional.of(userEntity));

        assertThat(repository.existsByIdAndRole("usr_1", Role.TRADER)).isFalse();
    }
}

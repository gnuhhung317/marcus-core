package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.UserEntity;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPortfolioProvisioningServiceTest {

    @Mock
    private SpringDataUserRepository springDataUserRepository;

    @Mock
    private SpringDataUserPortfolioRepository springDataUserPortfolioRepository;

    @Test
    void backfillMissingPortfolios_createsRowsForUsersWithoutPortfolio() {
        UserPortfolioProvisioningService service = new UserPortfolioProvisioningService(
                springDataUserRepository,
                springDataUserPortfolioRepository
        );

        when(springDataUserRepository.findAll()).thenReturn(List.of(
                UserEntity.builder().userId("usr-1").build(),
                UserEntity.builder().userId("usr-2").build()
        ));
        when(springDataUserPortfolioRepository.findAll()).thenReturn(List.of(
                UserPortfolioEntity.builder().userId("usr-1").build()
        ));

        int created = service.backfillMissingPortfolios();

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<UserPortfolioEntity> captor = ArgumentCaptor.forClass(UserPortfolioEntity.class);
        verify(springDataUserPortfolioRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("usr-2");
        assertThat(captor.getValue().getDataFreshness()).isEqualTo("STALE");
    }
}

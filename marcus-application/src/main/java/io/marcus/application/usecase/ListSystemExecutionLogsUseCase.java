package io.marcus.application.usecase;

import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListSystemExecutionLogsUseCase {

    private final IdentityService identityService;
    private final PortfolioReadPort portfolioReadPort;

    public PortfolioReadPort.ExecutionLogPageSnapshot execute(String cursor, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        String userId = identityService.getCurrentUserId().orElse(null);
        Role role = identityService.getCurrentUserRole().orElse(null);
        return portfolioReadPort.listSystemExecutionLogs(cursor, normalizedLimit, userId, role);
    }
}

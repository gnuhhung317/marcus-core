package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.domain.port.AdminAuditEventPort;
import io.marcus.domain.port.AdminBotPort;
import io.marcus.domain.port.AdminSubscriptionPort;
import io.marcus.domain.port.AdminUserPort;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.vo.BotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardOverviewUseCase {

    private final AdminUserPort adminUserPort;
    private final AdminBotPort adminBotPort;
    private final AdminSubscriptionPort adminSubscriptionPort;
    private final AdminAuditEventPort adminAuditEventPort;
    private final GetSystemConnectivityHealthUseCase getSystemConnectivityHealthUseCase;

    public AdminDtos.SystemOverview execute() {
        var recentActions = adminAuditEventPort.search(null, null, null, null, 0, 10)
                .map(event -> new AdminDtos.AuditEventRow(
                        event.getAdminAuditEventId(),
                        event.getActorUserId(),
                        event.getAction(),
                        event.getTargetType(),
                        event.getTargetId(),
                        event.getReason(),
                        event.getBeforeStateJson(),
                        event.getAfterStateJson(),
                        event.getCreatedAt()
                ))
                .getContent();

        PortfolioReadPort.ConnectivityHealthSnapshot health = getSystemConnectivityHealthUseCase.execute();
        return new AdminDtos.SystemOverview(
                adminUserPort.countAll(),
                adminUserPort.countByBannedTrue(),
                adminBotPort.countAll(),
                adminBotPort.countByStatus(BotStatus.ACTIVE),
                adminBotPort.countByStatus(BotStatus.PAUSED),
                adminBotPort.countByStatus(BotStatus.DELETED),
                adminSubscriptionPort.countActive(),
                adminSubscriptionPort.countDisconnectedActiveExecutors(),
                recentActions,
                health.overallStatus(),
                health.checkedAt()
        );
    }
}

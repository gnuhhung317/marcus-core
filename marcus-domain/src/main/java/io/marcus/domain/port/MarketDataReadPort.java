package io.marcus.domain.port;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketDataReadPort {

    record DashboardOverviewSnapshot(
            double totalEquity,
            double openPnl,
            double winRate,
            int activeBots,
            int freshAccountsCount,
            int staleAccountsCount,
            String dataFreshness,
            LocalDateTime lastUpdated
    ) {}

    record ExchangeAllocationSnapshot(String exchange, double percentage) {}

    DashboardOverviewSnapshot getDashboardOverview(String userId);

    List<ExchangeAllocationSnapshot> listExchangeAllocation(String userId);
}

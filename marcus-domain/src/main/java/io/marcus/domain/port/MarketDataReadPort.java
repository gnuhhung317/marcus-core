package io.marcus.domain.port;

import java.util.List;

public interface MarketDataReadPort {

    record DashboardOverviewSnapshot(
            double totalEquity,
            double openPnl,
            double winRate,
            int activeBots
    ) {}

    record ExchangeAllocationSnapshot(String exchange, double percentage) {}

    DashboardOverviewSnapshot getDashboardOverview(String userId);

    List<ExchangeAllocationSnapshot> listExchangeAllocation(String userId);
}

package io.marcus.domain.port;

public interface PortfolioSyncPort {

    void syncBalance(PortfolioSyncContext context, PortfolioBalanceSyncData data);
}

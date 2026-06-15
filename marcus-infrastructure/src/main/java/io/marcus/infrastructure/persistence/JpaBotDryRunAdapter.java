package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.BotDryRunClosedTrade;
import io.marcus.domain.model.BotDryRunPortfolioPoint;
import io.marcus.domain.model.BotDryRunPosition;
import io.marcus.domain.model.BotDryRunState;
import io.marcus.domain.port.BotDryRunPort;
import io.marcus.infrastructure.persistence.entity.BotDryRunClosedTradeEntity;
import io.marcus.infrastructure.persistence.entity.BotDryRunPortfolioEntity;
import io.marcus.infrastructure.persistence.entity.BotDryRunPositionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JpaBotDryRunAdapter implements BotDryRunPort {

    static final String OPEN = "OPEN";
    static final String CLOSED_OR_STALE = "CLOSED_OR_STALE";
    static final String OUT_OF_SAMPLE = "OUT_OF_SAMPLE";

    private final SpringDataBotDryRunPortfolioRepository portfolioRepository;
    private final SpringDataBotDryRunPositionRepository positionRepository;
    private final SpringDataBotDryRunClosedTradeRepository closedTradeRepository;

    @Override
    @Transactional
    public BotDryRunState syncSnapshot(BotDryRunState state) {
        if (state == null || state.portfolio() == null) {
            throw new IllegalArgumentException("dry-run state is required");
        }

        BotDryRunPortfolioEntity portfolioEntity = portfolioRepository
                .findByBotIdAndTimestamp(state.portfolio().botId(), state.portfolio().timestamp())
                .orElseGet(BotDryRunPortfolioEntity::new);
        portfolioEntity.setBotId(state.portfolio().botId());
        portfolioEntity.setDataSource(OUT_OF_SAMPLE);
        portfolioEntity.setTimestamp(state.portfolio().timestamp());
        portfolioEntity.setCash(state.portfolio().cash());
        portfolioEntity.setEquity(state.portfolio().equity());
        portfolioEntity.setRealizedPnl(state.portfolio().realizedPnl());
        portfolioEntity.setUnrealizedPnl(state.portfolio().unrealizedPnl());
        portfolioEntity.setTotalFees(state.portfolio().totalFees());
        portfolioRepository.save(portfolioEntity);

        Set<String> activePositionIds = new HashSet<>();
        for (BotDryRunPosition position : state.positions()) {
            activePositionIds.add(position.positionId());
            BotDryRunPositionEntity entity = positionRepository
                    .findByBotIdAndPositionId(position.botId(), position.positionId())
                    .orElseGet(BotDryRunPositionEntity::new);
            entity.setBotId(position.botId());
            entity.setPositionId(position.positionId());
            entity.setDataSource(OUT_OF_SAMPLE);
            entity.setSymbol(position.symbol());
            entity.setMarketType(position.marketType());
            entity.setSide(position.side());
            entity.setQuantity(position.quantity());
            entity.setEntryPrice(position.entryPrice());
            entity.setCurrentPrice(position.currentPrice());
            entity.setUnrealizedPnl(position.unrealizedPnl());
            entity.setOpenedAt(position.openedAt());
            entity.setSourceSignalId(position.sourceSignalId());
            entity.setStatus(OPEN);
            entity.setLastSyncedAt(state.portfolio().timestamp());
            entity.setClosedAt(null);
            positionRepository.save(entity);
        }

        List<BotDryRunPositionEntity> existingPositions = positionRepository.findByBotIdOrderByOpenedAtAsc(state.portfolio().botId());
        for (BotDryRunPositionEntity entity : existingPositions) {
            if (OPEN.equals(entity.getStatus()) && !activePositionIds.contains(entity.getPositionId())) {
                entity.setStatus(CLOSED_OR_STALE);
                entity.setClosedAt(state.portfolio().timestamp());
                entity.setLastSyncedAt(state.portfolio().timestamp());
                positionRepository.save(entity);
            }
        }

        for (BotDryRunClosedTrade trade : state.closedTrades()) {
            BotDryRunClosedTradeEntity entity = closedTradeRepository
                    .findByBotIdAndTradeId(trade.botId(), trade.tradeId())
                    .orElseGet(BotDryRunClosedTradeEntity::new);
            entity.setBotId(trade.botId());
            entity.setTradeId(trade.tradeId());
            entity.setDataSource(OUT_OF_SAMPLE);
            entity.setSymbol(trade.symbol());
            entity.setMarketType(trade.marketType());
            entity.setSide(trade.side());
            entity.setQuantity(trade.quantity());
            entity.setEntryPrice(trade.entryPrice());
            entity.setExitPrice(trade.exitPrice());
            entity.setPnl(trade.pnl());
            entity.setFees(trade.fees());
            entity.setEntryTimestamp(trade.entryTimestamp());
            entity.setExitTimestamp(trade.exitTimestamp());
            entity.setEntrySignalId(trade.entrySignalId());
            entity.setExitSignalId(trade.exitSignalId());
            closedTradeRepository.save(entity);
        }

        return findLatestState(state.portfolio().botId()).orElse(new BotDryRunState(state.portfolio(), List.of(), List.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BotDryRunState> findLatestState(String botId) {
        return portfolioRepository.findTopByBotIdOrderByTimestampDesc(botId)
                .map(portfolio -> new BotDryRunState(
                        toPortfolio(portfolio),
                        positionRepository.findByBotIdAndStatusOrderByOpenedAtAsc(botId, OPEN).stream()
                                .map(this::toPosition)
                                .toList(),
                        closedTradeRepository.findByBotIdOrderByExitTimestampAsc(botId).stream()
                                .map(this::toClosedTrade)
                                .toList()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotDryRunPortfolioPoint> findPortfolioPoints(String botId) {
        return portfolioRepository.findByBotIdOrderByTimestampAsc(botId).stream()
                .map(this::toPortfolio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotDryRunClosedTrade> findClosedTrades(String botId) {
        return closedTradeRepository.findByBotIdOrderByExitTimestampAsc(botId).stream()
                .map(this::toClosedTrade)
                .toList();
    }

    private BotDryRunPortfolioPoint toPortfolio(BotDryRunPortfolioEntity entity) {
        return new BotDryRunPortfolioPoint(
                entity.getBotId(),
                entity.getTimestamp(),
                entity.getCash(),
                entity.getEquity(),
                entity.getRealizedPnl(),
                entity.getUnrealizedPnl(),
                entity.getTotalFees()
        );
    }

    private BotDryRunPosition toPosition(BotDryRunPositionEntity entity) {
        return new BotDryRunPosition(
                entity.getBotId(),
                entity.getPositionId(),
                entity.getSymbol(),
                entity.getMarketType(),
                entity.getSide(),
                entity.getQuantity(),
                entity.getEntryPrice(),
                entity.getCurrentPrice(),
                entity.getUnrealizedPnl(),
                entity.getOpenedAt(),
                entity.getSourceSignalId(),
                entity.getStatus()
        );
    }

    private BotDryRunClosedTrade toClosedTrade(BotDryRunClosedTradeEntity entity) {
        return new BotDryRunClosedTrade(
                entity.getBotId(),
                entity.getTradeId(),
                entity.getSymbol(),
                entity.getMarketType(),
                entity.getSide(),
                entity.getQuantity(),
                entity.getEntryPrice(),
                entity.getExitPrice(),
                entity.getPnl(),
                entity.getFees(),
                entity.getEntryTimestamp(),
                entity.getExitTimestamp(),
                entity.getEntrySignalId(),
                entity.getExitSignalId()
        );
    }
}

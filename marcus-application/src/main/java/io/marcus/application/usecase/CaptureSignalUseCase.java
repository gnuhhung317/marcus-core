package io.marcus.application.usecase;

import io.marcus.application.dto.CaptureSignalRequest;
import io.marcus.application.dto.ResolveBotRoutingTargetsRequest;
import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.exception.DuplicateSignalException;
import io.marcus.domain.model.Signal;
import io.marcus.domain.vo.MarginMode;
import io.marcus.domain.vo.MarketType;
import io.marcus.domain.vo.OrderType;
import io.marcus.domain.vo.SignalStatus;
import io.marcus.domain.port.SignalPublisherPort;
import io.marcus.domain.port.SignalServerDispatchPort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaptureSignalUseCase {

    private final SignalRepository signalRepository;
    private final BotRepository botRepository;
    private final ResolveBotRoutingTargetsUseCase resolveBotRoutingTargetsUseCase;
    private final SignalPublisherPort signalPublisherPort;
    private final SignalServerDispatchPort signalServerDispatchPort;

    public void execute(CaptureSignalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Signal request is required");
        }

        // --- Guard: bot must exist ---
        if (!botRepository.findByBotId(request.botId()).isPresent()) {
            throw new BotNotFoundException("Bot not found: " + request.botId());
        }

        // --- Guard: idempotency — reject duplicate signalId ---
        if (signalRepository.existsBySignalId(request.signalId())) {
            throw new DuplicateSignalException(request.signalId());
        }

        // --- Guard: LIMIT orders require an entry price ---
        OrderType effectiveOrderType = request.orderType() != null ? request.orderType() : OrderType.LIMIT;
        if (effectiveOrderType == OrderType.LIMIT && request.entry() == null) {
            throw new IllegalArgumentException(
                    "entry price is required for LIMIT orders (signalId=" + request.signalId() + ")"
            );
        }

        // --- Build domain object with defaults applied ---
        Signal signal = Signal.builder()
                .signalId(request.signalId())
                .botId(request.botId())
                .symbol(request.symbol())
                .action(request.action())
                .marketType(request.marketType() != null ? request.marketType() : MarketType.SPOT)
                .orderType(effectiveOrderType)
                .entry(request.entry())
                .stopLoss(request.stopLoss())
                .takeProfit(request.takeProfit())
                .amount(request.amount())
                .leverage(request.leverage() != null ? request.leverage() : 1)
                .marginMode(request.marginMode() != null ? request.marginMode() : MarginMode.CROSS)
                .reduceOnly(request.reduceOnly())
                .status(request.status() != null ? request.status() : SignalStatus.RECEIVED)
                .generatedTimestamp(request.generatedTimestamp() != null
                        ? request.generatedTimestamp()
                        : LocalDateTime.now())
                .timeframe(request.timeframe())
                .metadata(request.metadata())
                .build();

        // 1. Persist to PostgreSQL first
        signalRepository.save(signal);
        log.info("[Signal] Captured signalId={} botId={} action={} marketType={} orderType={}",
                signal.getSignalId(), signal.getBotId(), signal.getAction(),
                signal.getMarketType(), signal.getOrderType());

        // 2. Publish to Kafka (storage + global broadcast)
        signalPublisherPort.publish(signal);

        // 3. Resolve target servers and dispatch for real-time routing
        Set<String> targetServerIds = resolveBotRoutingTargetsUseCase
                .execute(new ResolveBotRoutingTargetsRequest(signal.getBotId()));

        if (targetServerIds.isEmpty()) {
            log.debug("[Signal] No active subscribers found for botId={}", signal.getBotId());
            return;
        }

        signalServerDispatchPort.dispatchToServers(signal, targetServerIds);
    }
}

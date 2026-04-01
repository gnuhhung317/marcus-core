package io.marcus.application.usecase;

import io.marcus.application.dto.MySubscriptionsResult;
import io.marcus.application.dto.SubscriptionSummaryResult;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMySubscriptionsUseCase {

    private static final String EXECUTOR_INSTRUCTION =
            "Copy WS_TOKEN and paste it into WS_TOKEN in the Local Executor .env file before starting the executor.";

    private final IdentityService identityService;
    private final UserRepository userRepository;
    private final UserSubscriptionPersistencePort userSubscriptionPersistencePort;
    private final BotRepository botRepository;

    public MySubscriptionsResult execute() {
        String currentUserId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (!userRepository.existsByIdAndRole(currentUserId, Role.USER)) {
            throw new ForbiddenOperationException("Only trader can view subscriptions");
        }

        List<UserSubscription> subscriptions = userSubscriptionPersistencePort.findActiveByUserId(currentUserId);
        List<SubscriptionSummaryResult> summaryResults = subscriptions.stream()
                .map(this::toSummary)
                .toList();

        String wsToken = subscriptions.stream()
                .map(UserSubscription::getWsToken)
                .filter(token -> token != null && !token.isBlank())
                .findFirst()
                .orElse(null);

        return MySubscriptionsResult.builder()
                .wsToken(wsToken)
                .localExecutorInstruction(EXECUTOR_INSTRUCTION)
                .subscriptions(summaryResults)
                .build();
    }

    private SubscriptionSummaryResult toSummary(UserSubscription subscription) {
        Bot bot = botRepository.findByBotId(subscription.getBotId()).orElse(null);

        return SubscriptionSummaryResult.builder()
                .botId(subscription.getBotId())
                .botName(bot != null ? bot.getName() : null)
                .tradingPair(bot != null ? bot.getTradingPair() : null)
                .status(subscription.getStatus().name())
                .subscribedAt(subscription.getStartDate())
                .build();
    }
}

package io.marcus.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@RequiredArgsConstructor
public class UserPortfolioBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserPortfolioBootstrapRunner.class);

    private final UserPortfolioProvisioningService userPortfolioProvisioningService;

    @Override
    public void run(String... args) {
        int created = userPortfolioProvisioningService.backfillMissingPortfolios();
        if (created > 0) {
            log.info("Bootstrapped {} missing user portfolio rows", created);
        }
    }
}

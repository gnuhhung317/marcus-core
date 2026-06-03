# CONTEXT — signal-core-backend (L1 Service)

> **Parent**: [CONTEXT_MAP.md](../CONTEXT_MAP.md) | **Changelog**: [CONTEXT_CHANGELOG.md](CONTEXT_CHANGELOG.md)
> **Role**: "The Translator & Guard" — Central CMS, Signal Router, Subscription Engine, Global Risk Management

---

## Service Identity

| Property | Value |
|----------|-------|
| Stack | Java 17, Spring Boot 3.x, Maven |
| Port | 8080 |
| Database | PostgreSQL 15+ (primary), Redis 7+ (cache/rate-limit) |
| Messaging | Kafka (optional, for event streaming) |
| Architecture | Clean Architecture / Hexagonal (Ports & Adapters) |
| Build | `mvn clean install` / `mvn -pl <module> -am test` |

---

## Module Structure (Maven Multi-Module)

```
signal-core-backend/
├── marcus-domain/          # Core domain: Entities, Value Objects, Ports (interfaces)
│   └── io.marcus.domain/
│       ├── model/          # Domain entities
│       ├── vo/             # Value objects
│       ├── port/           # Input/Output port interfaces
│       ├── repository/     # Repository interfaces
│       ├── service/        # Domain services
│       ├── executor/       # Execution-related domain
│       ├── mapper/         # Domain mappers
│       └── exception/      # Domain exceptions
│
├── marcus-application/     # Use cases / Application services
│   └── io.marcus.application/
│       └── usecase/        # Business use cases
│           ├── CaptureSignalUseCase
│           ├── UpdateBotStatusUseCase
│           ├── SubscribeBotUseCase
│           └── ListMySubscriptionsUseCase
│
├── marcus-infrastructure/  # Adapters: JPA repos, Redis, Kafka, external integrations
│   └── io.marcus.infrastructure/
│       └── integration/    # External service adapters
│
├── marcus-api/             # REST controllers, WebSocket handlers, security config
│   └── io.marcus.api/
│       ├── config/         # Security, WebSocket config
│       └── controller/     # REST controllers
│           ├── SignalController
│           ├── BotController
│           ├── SubscriptionController
│           └── RoutingController (lifecycle: DELETE endpoints for sessions/subscribers)
│
└── pom.xml                 # Parent POM
```

### Dependency Flow
```
marcus-api → marcus-application → marcus-domain ← marcus-infrastructure
```
Domain has NO dependencies on infrastructure or API.

---

## Key Concepts

### Signal Flow
1. **Ingest**: Bot sends Signal JSON via WebSocket/API → `SignalController`
2. **Validate**: `CaptureSignalUseCase` validates schema, checks bot registration
3. **Risk Check**: Global risk rules applied (position limits, exposure caps)
4. **Translate**: Signal → Execution Instruction (bar-to-timestamp, ATR offset calculation)
5. **Route**: Execution Instruction sent to subscriber's executor OR executed with delegated API key

### Subscriptions (KAN-39)
- `POST /api/v1/subscriptions/{botId}` — Trader subscribes to a bot
- `GET /api/v1/subscriptions/my-subscriptions` — List trader's subscriptions
- Schema: `V2__subscription_schema.sql`

### Routing Lifecycle
- `DELETE /routing/sessions` — Remove routing session
- `DELETE /routing/subscribers` — Remove subscriber
- Keep HMAC interceptor path patterns in sync when adding new routing endpoints

---

## Important Patterns & Gotchas

1. **Maven module runs**: Always use `mvn -pl <module> -am test` to ensure upstream modules are built. Running `mvn -pl marcus-infrastructure test` alone causes false package-not-found errors if SNAPSHOT dependencies are stale.

2. **Maven specific tests**: `mvn -pl marcus-api,marcus-application,marcus-infrastructure -am test -DskipITs "-Dtest=TestName" "-Dsurefire.failIfNoSpecifiedTests=false"`

3. **PowerShell gotcha**: If `-Dsurefire.failIfNoSpecifiedTests=false` is parsed incorrectly, wrap in quotes.

4. **Lombok false positives**: VS Code Java diagnostics can show missing getters/builders/log errors even when Maven compile/test succeeds. Trust Maven output over editor.

5. **Mockito strict stubbing**: In Redis adapter tests, avoid fixed List argument stubs from Set iteration; use `thenAnswer`/`anyList()` or deterministic collections.

6. **Kafka**: Port 9092 not reachable from dev machine. E2E ingestion validation depending on Kafka is blocked until 9092 opens.

7. **Infrastructure reachability** (from dev machine): PostgreSQL 5432 ✅, Redis 6379 ✅, Kafka 9092 ❌

---

## Database

- **Engine**: PostgreSQL 15+
- **Migrations**: Flyway (check `marcus-infrastructure/src/main/resources/db/migration/`)
- **Key tables**: users, bots, signals, subscriptions, execution_instructions, routing_sessions, bot_backtest_runs, bot_historical_portfolios, bot_historical_closed_trades, bot_dry_run_portfolios, bot_dry_run_positions, bot_dry_run_closed_trades, bot_telemetry_points

---

## How to Run

```bash
# Full build
mvn clean install

# Run tests for specific module
mvn -pl marcus-api -am test

# Run specific test class
mvn -pl marcus-api,marcus-application,marcus-infrastructure -am test -DskipITs "-Dtest=CaptureSignalUseCaseTest"

# Start (requires PostgreSQL & Redis running)
mvn -pl marcus-api spring-boot:run
```

---

## Contract Dependencies

- **AsyncAPI**: Signal transport contract (source of truth for Bot ↔ Backend protocol)
- **OpenAPI**: REST API specs in `docs/openapi/`
- **Signal Schema**: See [docs/architecture/SYSTEM_OVERVIEW.md](../docs/architecture/SYSTEM_OVERVIEW.md) §5

---

## Delegation Rules

- When bot-framework-python needs contract changes → update this service first, then SDK
- Credential/auth policy changes → always decided here first

---

> **Update Trigger**: When adding new controllers, use cases, changing DB schema, or modifying signal/execution contract → update this CONTEXT.md and append to CONTEXT_CHANGELOG.md
### Bot Lifecycle Analytics
1. **Historical backtest upload**: SDK batch posts `BacktestReport` to `POST /api/v1/bots/{botId}/backtest-results`.
2. **Live dry-run sync**: SDK streams paper-trading state to `POST /api/v1/bots/{botId}/dry-run/sync`.
3. **Operational telemetry**: `POST /api/v1/bots/{botId}/telemetry` stores non-PnL metrics only.
4. **Analytics merge**: `GetBotAnalyticsUseCase` merges `bot_historical_portfolios` and `bot_dry_run_portfolios` into one performance series. Historical rows use `data_source = HISTORICAL`; live rows use `data_source = OUT_OF_SAMPLE`.
5. Do not reintroduce mock historical analytics. If a bot has no persisted backtest or dry-run data, analytics should return an empty or limited series with warnings rather than fabricated points.
### Routing Lifecycle

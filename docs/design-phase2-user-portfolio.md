# Phase 2 Design: UserPortfolio & Executor Balance Sync

> **Status**: Design Complete — Implementation Pending (post Phase 1 refactor)  
> **Author**: Marcus Team  
> **Date**: 2026-05-10  
> **Depends on**: Phase 1 (StaticTerminalReadAdapter refactor)

---

## 1. Problem Statement

Hiện tại `StaticTerminalReadAdapter` sử dụng hardcoded capital (`10_000.0`) và các ngưỡng rủi ro cố định (`0.25`, `0.12`) để tính toán Dashboard Overview, Portfolio metrics, và Decision Scoring.

Điều này dẫn đến:
- **Dữ liệu tài chính không phản ánh thực tế** — tất cả users đều thấy cùng một vốn cơ sở.
- **Risk threshold không tùy chỉnh được** — mỗi trader có ngưỡng chịu rủi ro khác nhau.
- **Không tận dụng được data thực** mà Executor (Python Client) đã có khả năng lấy từ Exchange (Binance).

### Hiện trạng Data Model

```
UserEntity
├── userId (PK)
├── username
├── email
├── passwordHash
├── role
└── ❌ KHÔNG CÓ: capital, balance, pnl, risk_threshold
```

### Hiện trạng Luồng Executor ↔ Backend

```
Executor (Python)                    Backend (Java)
──────────────                       ──────────────
exchange.fetch_balance()             ❌ Không nhận
exchange.create_order()  ──WS──►     ExecutionEvent (ORDER_*, POSITION_*)
                                     RawEventEntity (audit trail)
```

Executor Python đã có khả năng gọi `exchange.fetch_balance()` qua CCXT, nhưng **dữ liệu balance/PnL không được sync ngược về backend**.

---

## 2. Proposed Solution

### 2.1 New Entity: `UserPortfolioEntity`

```java
@Entity
@Table(name = "user_portfolios",
       indexes = {
           @Index(name = "idx_user_portfolios_user_id", columnList = "user_id", unique = true)
       })
public class UserPortfolioEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    /**
     * Total deposited capital across all exchanges.
     * Initially set by user on onboarding, updated via executor sync.
     */
    @Column(name = "total_capital", precision = 18, scale = 8,
            nullable = false, columnDefinition = "NUMERIC(18,8) DEFAULT 0")
    private BigDecimal totalCapital;

    /**
     * Last known available (free) balance from exchange.
     * Synced from executor via audit-push balance_snapshot.
     */
    @Column(name = "available_balance", precision = 18, scale = 8,
            columnDefinition = "NUMERIC(18,8) DEFAULT 0")
    private BigDecimal availableBalance;

    /**
     * Cumulative realized PnL from all closed positions.
     * Computed from POSITION_CLOSED execution events.
     */
    @Column(name = "realized_pnl", precision = 18, scale = 8,
            columnDefinition = "NUMERIC(18,8) DEFAULT 0")
    private BigDecimal realizedPnl;

    /**
     * Current unrealized PnL from open positions.
     * Updated via executor balance sync.
     */
    @Column(name = "unrealized_pnl", precision = 18, scale = 8,
            columnDefinition = "NUMERIC(18,8) DEFAULT 0")
    private BigDecimal unrealizedPnl;

    /**
     * User-configured max drawdown threshold for HIGH_RISK alerts.
     * Default: 0.10 (10%). User can customize via Settings page.
     */
    @Column(name = "max_drawdown_threshold", precision = 5, scale = 4,
            nullable = false, columnDefinition = "NUMERIC(5,4) DEFAULT 0.1000")
    private BigDecimal maxDrawdownThreshold;

    /**
     * User-configured medium risk threshold for NEEDS_REVIEW alerts.
     * Default: 0.05 (5%).
     */
    @Column(name = "medium_risk_threshold", precision = 5, scale = 4,
            nullable = false, columnDefinition = "NUMERIC(5,4) DEFAULT 0.0500")
    private BigDecimal mediumRiskThreshold;

    /**
     * Exchange identifier this portfolio tracks (e.g., "binance").
     */
    @Column(name = "exchange_id")
    private String exchangeId;

    /**
     * Timestamp of last successful balance sync from executor.
     */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
}
```

### SQL Migration

```sql
CREATE TABLE user_portfolios (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL UNIQUE,
    total_capital         NUMERIC(18,8) NOT NULL DEFAULT 0,
    available_balance     NUMERIC(18,8) DEFAULT 0,
    realized_pnl          NUMERIC(18,8) DEFAULT 0,
    unrealized_pnl        NUMERIC(18,8) DEFAULT 0,
    max_drawdown_threshold NUMERIC(5,4) NOT NULL DEFAULT 0.1000,
    medium_risk_threshold  NUMERIC(5,4) NOT NULL DEFAULT 0.0500,
    exchange_id           VARCHAR(255),
    last_sync_at          TIMESTAMP,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at            TIMESTAMP
);

CREATE UNIQUE INDEX idx_user_portfolios_user_id ON user_portfolios(user_id);
```

---

### 2.2 Balance Sync Flow: Executor → Backend

Tận dụng kênh WebSocket và `audit-push` message type đã có trong `RawEventEntity`.

#### Sequence Diagram

```
Executor (Python)                Backend (Java)                    DB
─────────────────                ──────────────                    ──
                                 
  [Mỗi 60 giây]
  │
  ├──► exchange.fetch_balance()
  │    ← {total: 12500, free: 8200, used: 4300}
  │
  ├──► exchange.fetch_positions() (nếu có)
  │    ← [{unrealizedPnl: +340.50}, ...]
  │
  ├──► WebSocket: audit-push frame
  │    {
  │      "type": "audit-push",
  │      "payload": {
  │        "kind": "balance_snapshot",
  │        "total": 12500.0,
  │        "free": 8200.0,
  │        "used": 4300.0,
  │        "unrealizedPnl": 340.50,
  │        "exchange": "binance",
  │        "timestamp": "2026-05-10T16:00:00Z"
  │      }
  │    }
  │                              │
  │                              ├──► RawEventEntity.save() (audit trail)
  │                              │
  │                              ├──► BalanceSyncUseCase.execute()
  │                              │    │
  │                              │    ├──► userPortfolioRepo
  │                              │    │    .findByUserId(userId)
  │                              │    │    or create new
  │                              │    │
  │                              │    ├──► UPDATE user_portfolios
  │                              │    │    SET available_balance = 8200,
  │                              │    │        unrealized_pnl = 340.50,
  │                              │    │        last_sync_at = NOW()
  │                              │    │    WHERE user_id = :userId
  │                              │    │                    ──────────► DB
```

#### Python Client Changes (`ws_client.py`)

```python
class ExecutorWebSocketClient:
    BALANCE_SYNC_INTERVAL = 60  # seconds

    async def _balance_sync_loop(self):
        """Periodic balance sync to backend via audit-push."""
        while self._connected:
            try:
                balance = await asyncio.to_thread(
                    self._exchange.fetch_balance
                )
                positions = await asyncio.to_thread(
                    self._exchange.fetch_positions
                )
                unrealized = sum(
                    float(p.get("unrealizedPnl", 0))
                    for p in positions
                    if p.get("unrealizedPnl")
                )

                await self._send_frame({
                    "type": "audit-push",
                    "payload": {
                        "kind": "balance_snapshot",
                        "total": float(balance.get("total", {}).get("USDT", 0)),
                        "free": float(balance.get("free", {}).get("USDT", 0)),
                        "used": float(balance.get("used", {}).get("USDT", 0)),
                        "unrealizedPnl": unrealized,
                        "exchange": self._config.exchange_id,
                        "timestamp": datetime.utcnow().isoformat(),
                    }
                })
            except Exception as e:
                self._logger.warning("Balance sync failed: %s", e)
            
            await asyncio.sleep(self.BALANCE_SYNC_INTERVAL)
```

#### Java Backend Changes

**New UseCase**: `BalanceSyncUseCase.java` (in `marcus-application`)

```java
@Service
@RequiredArgsConstructor
public class BalanceSyncUseCase {
    private final UserPortfolioRepository userPortfolioRepository;

    @Transactional
    public void execute(String userId, BalanceSyncPayload payload) {
        UserPortfolio portfolio = userPortfolioRepository
            .findByUserId(userId)
            .orElseGet(() -> UserPortfolio.createDefault(userId));

        portfolio.updateBalance(
            payload.availableBalance(),
            payload.unrealizedPnl(),
            payload.exchange()
        );

        userPortfolioRepository.save(portfolio);
    }
}
```

**Modify**: `ExecutorWebSocketHandler.java` — route `audit-push` with `kind=balance_snapshot` to `BalanceSyncUseCase`.

---

### 2.3 Impact on StaticTerminalReadAdapter

Sau khi Phase 2 implement xong, các method sau sẽ thay đổi:

| Method | Hiện tại (Phase 1) | Sau Phase 2 |
|---|---|---|
| `getDashboardOverview()` | `INITIAL_CAPITAL_PLACEHOLDER` (10k) | `userPortfolioRepo.findByUserId(userId).totalCapital` |
| `getPortfolioOverview()` | Hardcoded `10_000.0` | Real `totalCapital + unrealizedPnl` |
| `determineReason()` | Hardcoded thresholds `0.10`, `0.05` | `portfolio.maxDrawdownThreshold`, `portfolio.mediumRiskThreshold` |
| `calculateCurrentPnL()` | Tính từ signal returns × 10k | `portfolio.realizedPnl + portfolio.unrealizedPnl` |

---

### 2.4 User-Configurable Risk Thresholds

Frontend Settings page sẽ cho phép user điều chỉnh:
- **Max Drawdown Threshold** (HIGH_RISK trigger): default 10%, range 5%–50%
- **Medium Risk Threshold** (NEEDS_REVIEW trigger): default 5%, range 2%–25%

API endpoint:
```
PUT /api/v1/users/me/portfolio/risk-config
{
  "maxDrawdownThreshold": 0.15,
  "mediumRiskThreshold": 0.08
}
```

---

## 3. Implementation Checklist (Phase 2)

- [ ] Create `UserPortfolioEntity` + migration SQL
- [ ] Create `UserPortfolioRepository` (domain port)
- [ ] Create `SpringDataUserPortfolioRepository` (infra adapter)
- [ ] Create `BalanceSyncUseCase` (application layer)
- [ ] Modify `ExecutorWebSocketHandler` to route `balance_snapshot` audit-push
- [ ] Modify Executor Python `ws_client.py` — add `_balance_sync_loop()`
- [ ] Modify `StaticTerminalReadAdapter` — replace `INITIAL_CAPITAL_PLACEHOLDER` with real portfolio data
- [ ] Modify `determineReason()` — use user-configurable thresholds
- [ ] Add `PUT /users/me/portfolio/risk-config` endpoint
- [ ] Add portfolio onboarding on first login (create default UserPortfolio)

---

## 4. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Executor offline → stale balance | Show `lastSyncAt` on Dashboard, warn if > 5 min stale |
| Exchange API rate limits on `fetch_balance` | 60s interval well within Binance limit (1200/min) |
| Multiple executors for same user | Last-write-wins on `user_portfolios`, acceptable for MVP |
| User has no executor connected yet | Fallback to `totalCapital` (user-set) with `unrealizedPnl = 0` |

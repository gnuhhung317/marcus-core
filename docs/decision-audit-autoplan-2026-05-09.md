# Autoplan Review - Decision Audit Sync with External Executor

Captured: 2026-05-09
Backend branch: HEAD (detached)
Frontend branch: main
Backend checkpoint: 65baa19
Frontend checkpoint: 391c682

## Plan Summary
Implement a two-stream architecture to keep decision state and audit history consistent when an external client executor pushes delayed, duplicate, or out-of-order events.

Stream A ingests executor events into an immutable raw log, then updates decision projection.
Stream B exposes immutable decision audit history to Decision Dashboard via cursor pagination.

## Phase 1 - CEO Review (Strategy and Scope)

### Premises (confirmed)
1. External executor can deliver out-of-order and duplicate events.
2. Decision UI requires deterministic replayable history.
3. Projection state and audit history are separate read concerns.
4. Decision changes must be traceable with before/after context.

### Alternatives considered
| Option | Description | Completeness | Pros | Cons | Decision |
|---|---|---:|---|---|---|
| A | Single table for current state + history columns | 4/10 | Fast to start | Hard replay, weak traceability | Rejected |
| B | Event log + projection + dedicated audit feed | 10/10 | Strong consistency and replay | More initial plumbing | Selected |
| C | UI subscribes directly to ingest stream | 3/10 | Real-time feeling | Noisy and non-deterministic UI | Rejected |

### Scope mode
Selective expansion approved for files in the direct blast radius of Decision Dashboard and ingest pipeline.

In scope:
- Raw ingest idempotency and persistence
- Decision projection update path
- Decision audit event persistence and query endpoint
- Dashboard client contract and UI audit feed rendering

Out of scope (deferred):
- Full event sourcing rewrite for all modules
- Cross-service distributed trace UI
- Multi-tenant audit partitioning

### Error and rescue registry
| Failure | User impact | Rescue |
|---|---|---|
| Duplicate executor push | Repeated audit entries | Idempotency key and unique index |
| Out-of-order event | Wrong state transitions | Sequence/timestamp guard + reorder strategy |
| Projection crash after raw persist | Partial update | Retry worker with checkpoint cursor |
| Audit query timeout | Dashboard lag | Cursor pagination and index strategy |

## Phase 2 - Design Review (UI and Interaction)

UI scope detected: yes

### UX decisions
1. Decision Dashboard never reads directly from ingest stream.
2. Audit panel shows immutable timeline with newest-first ordering.
3. Filters: botId, reason, status change type, time range.
4. Progressive loading via nextCursor, append mode.

### Required UI states
- Loading: skeleton rows and disabled filters.
- Empty: no events in current filter range.
- Error: inline message with retry action.
- Partial: some events loaded, next page failed.

### Design risks and fixes
| Risk | Severity | Fix |
|---|---|---|
| Mixing current state and audit in same card blocks | High | Separate summary cards from timeline panel |
| Missing source attribution | Medium | Show sourceType and sourceId on each row |
| Time confusion between eventTime and ingestedAt | Medium | Show both values with clear labels |

## Phase 3 - Engineering Review (Architecture, Tests, Risks)

### Architecture dependency diagram
CURRENT:
External Executor -> Signal ingest endpoint -> Signal handling -> Decision state (implicit)

TARGET:
External Executor
  -> signed ingest endpoint
    -> idempotency check
      -> raw_event_log (append-only)
      -> projection worker
         -> decision_projection_state
         -> decision_audit_log (append-only)

Decision Dashboard
  -> portfolio overview endpoint (projection state)
  -> portfolio decisions endpoint (projection state)
  -> decision audit endpoint (decision_audit_log cursor pagination)

### Backend components to update
- signal-core-backend/marcus-api/src/main/java/io/marcus/api/controller/SignalController.java
- signal-core-backend/marcus-api/src/main/java/io/marcus/api/controller/DashboardController.java
- signal-core-backend/marcus-domain/src/main/java/io/marcus/domain/port/TerminalReadPort.java
- signal-core-backend/marcus-infrastructure/src/main/java/io/marcus/infrastructure/integration/StaticTerminalReadAdapter.java
- signal-core-backend/marcus-infrastructure/src/main/java/io/marcus/infrastructure/integration/demo/DemoTerminalReadAdapter.java

### Frontend components to update
- marcus-nextjs/lib/contracts/types.ts
- marcus-nextjs/lib/contracts/endpoints.ts
- marcus-nextjs/lib/contracts/client.ts
- marcus-nextjs/app/terminal/decision/page.tsx
- marcus-nextjs/app/terminal/decision/subscription-cards.tsx

### API contract proposal
GET /api/v1/dashboard/portfolio/decision-audit
Query:
- cursor (optional)
- limit (default 20, max 100)
- botId (optional)
- reason (optional)
- transition (optional, ex: ACTIVE_TO_AT_RISK)
- from (optional ISO time)
- to (optional ISO time)

Response:
- items[]
  - eventId
  - correlationId
  - subscriptionId
  - botId
  - sourceType (EXECUTOR, USER, SYSTEM)
  - sourceId
  - actionType
  - oldState
  - newState
  - oldReason
  - newReason
  - eventTime
  - ingestedAt
  - sequenceNo
  - metadata
- nextCursor
- hasMore

### Test diagram and coverage plan
| Flow | Test type | Status | Required |
|---|---|---|---|
| Signed ingest accepted | Integration | Missing | Add |
| Duplicate ingest ignored | Integration | Missing | Add |
| Out-of-order events ordered correctly | Integration | Missing | Add |
| Projection emits audit on state change | Unit + Integration | Missing | Add |
| No audit emitted when state unchanged | Unit | Missing | Add |
| Audit endpoint cursor pagination | Integration | Missing | Add |
| Dashboard renders timeline and load more | Frontend e2e | Missing | Add |
| Dashboard auth header present for audit API | Frontend integration | Missing | Add |

### Failure modes registry
| Mode | Detection | Mitigation |
|---|---|---|
| Clock skew from executor | Compare eventTime vs ingestedAt drift | Prefer sequence ordering, keep both timestamps |
| CorrelationId missing | Validation reject with actionable error | Require field for decision-impact events |
| Metadata schema drift | Contract validation errors | Version metadata payload |
| Hot partition on single bot | Slow writes | Composite indexes and sharding strategy if needed |

## Phase 3.5 - DX Review (developer-facing)

DX scope detected: yes

### Developer journey map
1. Bot runtime signs payload
2. Executor posts event
3. Backend validates and stores raw event
4. Worker updates projection
5. Worker writes decision audit event
6. Dashboard queries audit page
7. Operator investigates timeline
8. Developer replays by cursor
9. Team debugs via correlationId

### TTHW target
Current estimated TTHW for new integrator: 45-60 minutes.
Target after docs/examples: 10-15 minutes.

### DX checklist
- Provide sample signed payload for executor.
- Provide error catalog with problem, cause, fix.
- Provide replay guide using cursor and correlationId.
- Provide migration note for clients lacking correlationId.

## Cross-phase themes
1. Determinism over immediacy: UI should consume stable read model, not ingest stream.
2. Traceability over convenience: keep immutable logs with before/after states.
3. Idempotency first: external executor reliability assumptions are weak by default.

## Decision Audit Trail
| # | Phase | Decision | Classification | Principle | Rationale | Rejected |
|---:|---|---|---|---|---|---|
| 1 | CEO | Separate ingest stream and audit feed | Mechanical | Completeness | Prevent state/history coupling drift | Single-table design |
| 2 | CEO | Keep append-only raw log | Mechanical | Explicit over clever | Simplifies replay and debugging | Overwriting events |
| 3 | CEO | Cursor-based audit API | Mechanical | Pragmatic | Stable pagination for timeline | Offset-only pagination |
| 4 | Design | Timeline panel distinct from decision cards | Taste | Explicit over clever | Better operator scanning | Mixed card+timeline block |
| 5 | Eng | Emit audit only on meaningful state/reason change | Mechanical | DRY | Avoid noisy logs | Emit on every ingest |
| 6 | Eng | Require correlationId for decision-impact events | User Challenge | Completeness | Stronger cross-system traceability | Optional correlationId |
| 7 | DX | Publish executor integration examples | Mechanical | Bias toward action | Reduces onboarding time | Docs-later approach |

## Final Approval Gate

Status: DONE_WITH_CONCERNS

Concerns:
1. Backend branch is detached HEAD, shipping follow-up changes should move to a named branch before merge.
2. Existing deleted tool artifacts in backend history remain in prior checkpoint by user decision.
3. Audit persistence schema is proposed but not implemented yet.

User Challenge:
- Challenge 1: correlationId should be mandatory for decision-impact events.
  If rejected, operational debugging cost increases significantly during incident response.

Recommended next action:
1. Implement backend audit endpoint and storage first.
2. Wire frontend timeline against new endpoint.
3. Add integration tests for duplicate and out-of-order ingest.
4. Run end-to-end verification with external executor sample payloads.

## Executor WebSocket Protocol (spec)

This section defines the WebSocket protocol we will use on the existing executor->backend connection.

Purpose:
- Reuse the persistent WS connection the executor already opens to the backend.
- Turn that connection into a bidirectional ingest+control+audit channel.
- Ensure every mutating message is persisted append-only for audit and idempotency.

Handshake
- URL: `wss://<backend>/ws/executor`
- Executor must send a `handshake` JSON frame immediately after connect:

```json
{
  "type": "handshake",
  "botId": "<bot-id>",
  "timestamp": "2026-05-09T...Z",
  "payload": { "nonce": "uuid", "version": "1.0" },
  "signature": "HMAC-SHA256(botId|timestamp|base64(payload), botSecret)"
}
```

- Server validates signature by looking up the `botId` secret and verifying the HMAC covers **all three fields**: botId, timestamp, and payload. This prevents metadata tampering even if an attacker obtains a valid payload signature.
- Timestamp validation: server rejects if `timestamp` is older than 5 minutes (configurable) to prevent replay attacks.
- On success, server replies with `handshake-ack` including `connId` and server metadata. On failure, server rejects the connection with 401 code and logs the failure.

Envelope (every message)
- All frames are JSON with this minimal envelope:

```json
{
  "type": "ingest|ack|heartbeat|replay-request|replay-response|control|audit-push",
  "eventId": "uuid",
  "idempotencyKey": "string",    // MANDATORY for mutating messages
  "correlationId": "string",     // MANDATORY for decision-impact messages
  "timestamp": "ISO-8601",
  "payload": { ... }
}
```

Message types
- `ingest` (executor->backend): signal payload, metadata. Persist to `raw_event` immediately.
- `ack` (backend->executor): acknowledges `eventId` and `idempotencyKey`, includes `status` and optional `error`.
- `heartbeat`: keepalive.
- `replay-request` (executor->backend): request historical events, payload `{ "correlationId": "...", "fromSeq": N, "toSeq": M }` or timestamp range.
- `replay-response` (backend->executor): stream of historical `raw_event` items; marked with `replayChunk` and `endOfReplay`.
- `control`: operational commands (pause/resume/throttle/drain). Persisted and auditable.
- `audit-push` (backend->executor): DecisionAuditEvent pushes produced by projection; used for low-latency feedback to executor.

Business use cases (requirements)
1) Reliable delivery and audit: backend persists every `ingest` to `raw_event` with `receivedAt`, `sourceConnId`, `eventId`, `idempotencyKey`, `correlationId`.
2) Idempotency: duplicate retries using same `idempotencyKey` must be deduped by storage unique constraint and acked idempotently.
3) Replay for reconciliation: executor or operator can request replay by `correlationId`/seq range to reconcile local state.
4) Control plane: operator can `pause`/`resume` executor processing and request `drain` for safe maintenance.
5) Low-latency audit pushes: backend may push `audit-push` frames when projection emits decision changes; executor may subscribe to these for immediate reaction.

Persistence model (summary)
- `raw_event` table/entity fields: `id`, `event_id`, `bot_id`, `idempotency_key`, `correlation_id`, `type`, `payload` (JSON), `received_at`, `source_conn_id`, `sequence_no`, `processed`, `processed_at`.
- Unique index: (`bot_id`, `idempotency_key`). Sequence is monotonic per-bot to support replay windows.

Operational notes
- Require `correlationId` and `idempotencyKey` for mutating messages.
- **HMAC handshake security:** Signature MUST cover botId, timestamp, and payload (not just payload). Timestamp must be within 5 minutes of server time to prevent replay attacks.
- Implement heartbeat + server-side disconnect on missed heartbeats.
- Backend should publish `ack` immediately after persisting `raw_event` (sync ack) or after enqueue (async), but must include `status` to indicate persistence.
- Log all handshake failures (invalid signature, expired timestamp, missing botId) for security monitoring.

Next implementation steps (short)
- Add `ExecutorWsHandler` in `marcus-api` to accept authenticated WS connections and route frames to `CaptureSignalUseCase`.
- Add `RawEventEntity` + `SpringDataRawEventRepository` in `marcus-infrastructure` and an initial DB migration.
- Update `CaptureSignalUseCase` to validate envelope, persist the `raw_event`, enforce idempotency, and return `ack` frames.
- Add projection hook to emit `decision_audit` records and an `audit-push` publisher that sends pushes over the same connection when appropriate.

# CONTEXT CHANGELOG — signal-core-backend

> Track all significant context changes. Newest entries at top.
> Rules: See [CONTEXT_RULES.md](../CONTEXT_RULES.md)

---

<!-- Entry template:
## [YYYY-MM-DD] <short-title>
**Agent**: <agent-name>
**Type**: feature | fix | architecture | contract | gotcha
**What Changed**: <one-line summary>
**Why**: <reason>
**Impact**: <affected services/files>
**Action Required**: <migration/awareness>
-->

## [2026-06-03] Bot Lifecycle Analytics Dual-Pipeline
**Agent**: codex
**Type**: architecture
**What Changed**: Documented the historical backtest upload flow, live dry-run sync flow, and telemetry split as one bot lifecycle across SDK and backend docs.
**Why**: The backend now persists the full strategy lifecycle instead of relying on local-only backtests and mock historical analytics.
**Impact**: bot-framework-python changelog, signal-core-backend CONTEXT.md, analytics endpoints, and lifecycle tables.
**Action Required**: Use `/api/v1/bots/{botId}/backtest-results` for batch uploads, `/api/v1/bots/{botId}/dry-run/sync` for paper-trading sync, and `/api/v1/bots/{botId}/telemetry` only for operational metrics.

## [2026-06-02] Context Map System Created
**Agent**: system-setup
**Type**: architecture
**What Changed**: Created L1 CONTEXT.md for signal-core-backend as part of layered Context Map system
**Why**: Enable consistent agent onboarding and context preservation across sessions
**Impact**: No code changes — documentation only
**Action Required**: None — future changes should append entries here
